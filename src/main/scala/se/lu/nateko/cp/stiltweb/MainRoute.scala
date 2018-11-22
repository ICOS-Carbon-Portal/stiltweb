package se.lu.nateko.cp.stiltweb

import java.time.LocalDateTime
import java.time.LocalDate

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport
import se.lu.nateko.cp.stiltweb.marshalling.StationInfoMarshalling

class MainRoute(config: StiltWebConfig, cluster: StiltClusterApi) {

	import StiltJsonSupport._
	val authRouting = new AuthRouting(config.auth)
	import authRouting.user

	private val service = new StiltResultsPresenter(config)

	def route: Route = pathPrefix("viewer") {
		get {
			path("footprint") {
				parameters(("stationId", "footprint")) { (stationId, localDtStr) =>
					val localDt = LocalDateTime.parse(localDtStr)
					complete(service.getFootprintRaster(stationId, localDt))
				}
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			path("listfootprints") {
				parameters(("stationId", "fromDate", "toDate")) { (stationId, fromDateStr, toDateStr) =>
					val fromDate = LocalDate.parse(fromDateStr)
					val toDate = LocalDate.parse(toDateStr)
					val footprintsList = service.listFootprints(stationId, fromDate, toDate)
					complete(footprintsList.map(_.toString).toSeq)
				}
			} ~
			path("stationinfo") {
				import StationInfoMarshalling.stationInfoMarshaller
				complete(service.getStationInfos)
			} ~
			path("availablemonths") {
				complete(service.availableInputMonths)
			} ~
			redirectToTrailingSlashIfMissing(StatusCodes.Found){
				pathSingleSlash {
					complete(views.html.ViewerPage(config.auth))
				}
			}
		} ~
		post {
			path("stiltresult") {
				entity(as[StiltResultsRequest]) { req =>
					complete(service.getStiltResults(req).toSeq)
				}
			}
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
	get{
		pathEndOrSingleSlash{
			redirect("/viewer/", StatusCodes.Found)
		} ~
		path("buildInfo"){
			complete(BuildInfo.toString)
		} ~
		path("whoami"){
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
}
