package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshaller.fromStatusCodeAndValue
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.marshalling.{ToResponseMarshallable => TRM}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltweb.AtmoAccessClient.AppInfo
import se.lu.nateko.cp.stiltweb.marshalling.StationInfoMarshalling
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport.given
import se.lu.nateko.cp.stiltweb.zip.PackagingThrottler
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
import StiltResultsPresenter.{ResultRelPath, StiltResRelPath}

class MainRoute(config: StiltWebConfig, cluster: StiltClusterApi):

	private val authRouting = new AuthRouting(config.auth)
	import cluster.atmoClient
	import authRouting.{user, userReq}

	private val service = new StiltResultsPresenter(config)
	private val throttler = new PackagingThrottler[ResultRelPath](using cluster.dispatcher)

	given Unmarshaller[String, LocalDate] = Unmarshaller.apply[String, LocalDate](_ => s =>
		Future.fromTry(Try(LocalDate.parse(s)))
	)

	given [T: RootJsonWriter]: ToEntityMarshaller[T] = SprayJsonSupport.sprayJsonMarshaller

	val resultBatchSpec: Directive1[ResultBatch] =
		parameters("stationId", "fromDate".as[LocalDate], "toDate".as[LocalDate]).as(ResultBatch.apply _).or:
			complete(StatusCodes.BadRequest -> "Expected 'stationId', 'fromDate', and 'toDate' URL parameters")

	def route: Route = pathPrefix("viewer"){
		get{
			path("footprint"){
				parameters("stationId", "footprint"): (stationId, localDtStr) =>
					val localDt = LocalDateTime.parse(localDtStr)
					complete(service.getFootprintRaster(stationId, localDt))
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			path("listfootprints"):
				resultBatchSpec: batch =>
					val startD = Instant.now()
					val footprintsList = service.listFootprints(batch)
					complete(footprintsList.map(_.toString).toSeq)
			~
			(path("joinfootprints") & userReq){ user =>
				resultBatchSpec: batch =>
					throttler
						.runFor(user, batch.stationId):
							service.packageResults(batch)(using cluster.ioDispatcher)
						.fold(
							msg => complete(StatusCodes.ServiceUnavailable -> msg),
							zipPathFut =>
								val startTime = Instant.now()
								withRequestTimeout(5.minutes):
									onSuccess(zipPathFut): zipPath =>
										logAppInfo(startTime, user, "Packaging", Some(batch), zipPath)
										complete(service.listResultPackages(batch).get)
						)
			} ~
			pathPrefix("downloadresults" / StiltResRelPath): relPath =>
				userReq: user =>
					val startDt = Instant.now()
					onResponseStreamed(() => logAppInfo(startDt, user, "Downloading", None, relPath)):
						getFromFile(service.toResultPath(relPath).toFile)
			~
			path("listresultpackages"):
				resultBatchSpec: batch =>
					complete(service.listResultPackages(batch).get)
			~
			path("stationinfo") {
				import StationInfoMarshalling.given
				complete(service.getStationInfos)
			} ~
			path("availablemonths"){
				complete(service.availableInputMonths())
			} ~
			redirectToTrailingSlashIfMissing(StatusCodes.Found):
				pathSingleSlash:
					user{userId =>
						complete(views.html.ViewerPage(config.auth))
					} ~
					complete(views.html.LoginPage(config.auth, config.atmoAccess))
		} ~
		post:
			entity(as[StiltResultsRequest]): req =>
				withRequestTimeout(5.minutes):
					path("stiltresult"):
						complete(service.getStiltResults(req).toSeq)
					~
					path("stiltrawresult"):
						complete(service.getStiltRawResults(req).toSeq)
					~
					complete(StatusCodes.NotFound)
			~
			complete(StatusCodes.BadRequest -> "Expected a correct stilt result request JSON payload")
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
				userReq: userId =>
					entity(as[Job]){job =>
						if job.userId == userId.email then
							cluster.enqueueJob(job)
							complete(StatusCodes.OK)
						else
							complete((StatusCodes.Forbidden, "Wrong user id in the job definition!"))
					} ~
					complete((StatusCodes.BadRequest, "Wrong request payload, expected a proper Job object"))
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

	private def logAppInfo(startDt: Instant, user: UserId, action: String, batch: Option[ResultBatch], relPath: ResultRelPath) =
		import atmoClient.baseStiltUrl
		val basicInfo = AppInfo(
			user = user,
			startDate = startDt,
			endDate = Some(Instant.now()),
			resultUrl = s"$baseStiltUrl/viewer/downloadresults/$relPath",
			infoUrl = None,
			comment = Some(s"$action STILT results from $relPath")
		)
		val info = batch.fold(basicInfo): resBatch =>
			import resBatch.{stationId, fromDate, toDate}
			basicInfo.copy(
				infoUrl = Some(s"$baseStiltUrl/viewer/?stationId=${stationId}&fromDate=${fromDate}&toDate=${toDate}"),
				comment = Some(s"$action STILT results for station $stationId from $fromDate to $toDate")
			)
		atmoClient.log(info)
end MainRoute
