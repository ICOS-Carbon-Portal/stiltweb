package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshaller.fromStatusCodeAndValue
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.marshalling.{ToResponseMarshallable => TRM}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltweb.AtmoAccessClient.AppInfo
import se.lu.nateko.cp.stiltweb.marshalling.StationInfoMarshalling
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport.given
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonWriter

import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try

import SprayJsonSupport.sprayJsonUnmarshaller
import DefaultJsonProtocol.{StringJsonFormat, JsValueFormat, immSeqFormat}

class MainRoute(config: StiltWebConfig, cluster: StiltClusterApi) {

	val authRouting = new AuthRouting(config.auth)
	import cluster.atmoClient
	import authRouting.{user, userReq}

	private val service = new StiltResultsPresenter(config)

	given Unmarshaller[String, LocalDate] = Unmarshaller.apply[String, LocalDate](_ => s =>
		Future.fromTry(Try(LocalDate.parse(s)))
	)

	given [T: RootJsonWriter]: ToEntityMarshaller[T] = SprayJsonSupport.sprayJsonMarshaller

	def route: Route = pathPrefix("viewer"){
		get{
			path("footprint"){
				parameters("stationId", "footprint"){ (stationId, localDtStr) =>
					val localDt = LocalDateTime.parse(localDtStr)
					complete(service.getFootprintRaster(stationId, localDt))
				}
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			(path("listfootprints") & userReq){user =>
				parameters("stationId", "fromDate".as[LocalDate], "toDate".as[LocalDate]) { (stationId, fromDate, toDate) =>
					val startD = Instant.now()
					val footprintsList = service.listFootprints(stationId, fromDate, toDate)
					atmoClient.log(
						AppInfo(
							user = user,
							startDate = startD,
							endDate = Some(Instant.now()),
							resultUrl = s"https://stilt.icos-cp.eu/viewer/listfootprints?stationId=$stationId&fromDate=$fromDate&toDate=$toDate",
							infoUrl = None,
							comment = Some(s"Fetching a list of footprints for station $stationId")
						)
					)
					complete(footprintsList.map(_.toString).toSeq)
				}
			} ~
			path("joinfootprints") {
				parameters("stationId", "fromDate".as[LocalDate], "toDate".as[LocalDate]) { (stationId, fromDate, toDate) =>
					val netcdfPathFut = Future(service.mergeFootprintsToNetcdf(stationId, fromDate, toDate))(cluster.ioDispatcher)
					withRequestTimeout(5.minutes){
						onSuccess(netcdfPathFut){netcdf =>
							onResponseStreamed(() => Files.deleteIfExists(netcdf)){
								respondWithAttachment(stationId + ".nc"){
									getFromFile(netcdf.toFile)
								}
							}
						}
					}
				}
			} ~
			path("stationinfo") {
				import StationInfoMarshalling.given
				complete(service.getStationInfos)
			} ~
			path("availablemonths"){
				complete(service.availableInputMonths())
			} ~
			redirectToTrailingSlashIfMissing(StatusCodes.Found){
				pathSingleSlash {
					user{userId =>
						complete(views.html.ViewerPage(config.auth))
					} ~
					complete(views.html.LoginPage(config.auth, config.atmoAccess))
				}
			}
		} ~
		post {
			entity(as[StiltResultsRequest]) { req =>
				withRequestTimeout(5.minutes){
					path("stiltresult"){
						complete(service.getStiltResults(req).toSeq)
					} ~
					path("stiltrawresult") {
						complete(service.getStiltRawResults(req).toSeq)
					} ~
					complete(StatusCodes.NotFound)
				}
			} ~
			complete(StatusCodes.BadRequest -> "Expected a correct stilt result request JSON payload")
		}
	} ~
	pathPrefix("worker"){
		get {
			pathEnd{redirect("worker/", StatusCodes.Found)} ~
			pathSingleSlash {
				user{userId =>
					complete(views.html.WorkerPage(config.auth))
				} ~
				complete(views.html.LoginPage(config.auth, config.atmoAccess))
			} ~
			path("worker.js"){
				getFromResource("www/worker.js")
			} ~
			path("wsdashboardinfo"){
				handleWebSocketMessages(cluster.websocketsFlow)
			} ~
			pathPrefix("output"){
				getFromBrowseableDirectory(config.stateDirectory)
			}
		} ~
		post{
			path("enqueuejob"){
				userReq{userId =>
					entity(as[Job]){job =>
						if(job.userId == userId.email){
							cluster.enqueueJob(job)
							complete(StatusCodes.OK)
						}else{
							complete((StatusCodes.Forbidden, "Wrong user id in the job definition!"))
						}
					} ~
					complete((StatusCodes.BadRequest, "Wrong request payload, expected a proper Job object"))
				}
			} ~
			path("deletejob" / Segment) { jobId =>
				userReq{userId =>
					if (config.admins.exists(_ == userId.email)) {
						cluster.cancelJob(jobId)
						complete(StatusCodes.OK)
					} else {
						onSuccess(cluster.queryOwner(jobId)) {
							case Some(ownerId) =>
								if (userId.email == ownerId) {
									cluster.cancelJob(jobId)
									complete(StatusCodes.OK)
								} else {
									complete((StatusCodes.Forbidden, "User ID doesn't own job"))
								}
							case None =>
								complete((StatusCodes.BadRequest, "No such Job ID"))
						}
					}
				}
			}
		}
	} ~
	get{
		pathEndOrSingleSlash{
			redirect("/viewer/", StatusCodes.Found)
		} ~
		path("buildInfo"){
			complete(BuildInfo.toString)
		} ~
		path("whoami"){
			user{userId => complete(
				StatusCodes.OK -> WhoamiResult(userId.email, config.admins.exists(_ == userId.email))
			)} ~
			complete((StatusCodes.OK, WhoamiResult("")))
		} ~
		path("logout") {
			deleteCookie(config.auth.authCookieName, domain = config.auth.authCookieDomain, path = "/"){
				complete(StatusCodes.OK)
			}
		}
	}

	def onResponseStreamed(cb: () => Unit): Directive0 = mapResponseEntity(re => {
		re.transformDataBytes(Flow.apply[ByteString].watchTermination(){
			case (mat, fut) =>
				fut.onComplete{case _ => cb()}(cluster.ioDispatcher)
				mat
		})
	})

	def respondWithAttachment(fileName: String): Directive0 = respondWithHeader(
		`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> fileName))
	)

}
