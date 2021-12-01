package se.lu.nateko.cp.stiltweb

import java.nio.file.Files
import java.time.LocalDateTime
import java.time.LocalDate

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.marshalling.{ToResponseMarshallable => TRM}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.marshalling.Marshaller
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport
import se.lu.nateko.cp.stiltweb.marshalling.StationInfoMarshalling

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

class MainRoute(config: StiltWebConfig, cluster: StiltClusterApi) {

	import StiltJsonSupport._
	import scala.language.implicitConversions

	val authRouting = new AuthRouting(config.auth)
	import authRouting.user

	private val service = new StiltResultsPresenter(config)

	given Unmarshaller[String, LocalDate] = Unmarshaller.apply[String, LocalDate](_ => s =>
		Future.fromTry(Try(LocalDate.parse(s)))
	)

	given JsonWriter[String] = JsString(_)
	given [T](using jw: JsonWriter[T]): RootJsonWriter[Seq[T]] =
		DefaultJsonProtocol.immSeqFormat(DefaultJsonProtocol.lift(jw))

	given [T: RootJsonWriter]: ToEntityMarshaller[T] = SprayJsonSupport.sprayJsonMarshaller
	given [T: ToEntityMarshaller]: Conversion[T, TRM] = t => TRM(t)

	def route: Route = pathPrefix("viewer").apply{
		get.apply{
			path("footprint").apply{
				parameters("stationId", "footprint").apply{ (stationId, localDtStr) =>
					val localDt = LocalDateTime.parse(localDtStr)
					complete(service.getFootprintRaster(stationId, localDt))
				}
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			path("listfootprints").apply{
				parameters("stationId", "fromDate".as[LocalDate], "toDate".as[LocalDate]) { (stationId, fromDate, toDate) =>
					val footprintsList = service.listFootprints(stationId, fromDate, toDate)
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
				import StationInfoMarshalling.stationInfoMarshaller
				complete(service.getStationInfos)
			} ~
			path("availablemonths").apply{
				complete(service.availableInputMonths())
			} ~
			redirectToTrailingSlashIfMissing(StatusCodes.Found){
				pathSingleSlash {
					complete(views.html.ViewerPage(config.auth))
				}
			}
		} ~
		post.apply {
			entity(as[StiltResultsRequest]) { req =>
				withRequestTimeout(5.minutes).apply{
					path("stiltresult").apply{
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
				complete(views.html.WorkerPage(config.auth))
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
				user{userId =>
					entity(as[Job]){job =>
						if(job.userId == userId.email){
							cluster.enqueueJob(job)
							complete(StatusCodes.OK)
						}else{
							complete((StatusCodes.Forbidden, "Wrong user id in the job definition!"))
						}
					} ~
					complete((StatusCodes.BadRequest, "Wrong request payload, expected a proper Job object"))
				} ~
				complete((StatusCodes.Forbidden, "Please log in with Carbon Portal"))
			} ~
			path("deletejob" / Segment) { jobId =>
				user{userId =>
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
	get.apply{
		pathEndOrSingleSlash{
			redirect("/viewer/", StatusCodes.Found)
		} ~
		path("buildInfo"){
			complete(BuildInfo.toString)
		} ~
		path("whoami").apply{
			user{userId =>
				complete((StatusCodes.OK,
						  WhoamiResult(userId.email, config.admins.exists(_ == userId.email))))
			} ~
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
