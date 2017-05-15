package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltcluster.Job

class MainRoute(config: StiltWebConfig, cluster: StiltClusterApi) {

	import StiltJsonSupport._
	val authRouting = new AuthRouting(config.auth)
	import authRouting.user

	def viewerRoute(service: StiltResultsFetcher): Route = {
		get {
			path("footprint") {
				parameters("stationId", "footprint") { (stationId, filename) =>
					complete(service.getFootprintRaster(stationId, filename))
				}
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			path("listfootprints") {
				parameters("stationId", "year".as[Int]) { (stationId, year) =>
					complete(service.getFootprintFiles(stationId, year))
				}
			} ~
			path("stationinfo") {
				complete(service.getStationInfos)
			} ~
			redirectToTrailingSlashIfMissing(StatusCodes.Found){
				pathSingleSlash {
					complete(views.html.ViewerPage())
				}
			}
		} ~
		post {
			path("stiltresult") {
				entity(as[StiltResultsRequest]) { req =>
					val src = service.getStiltResultJson(req.stationId, req.year, req.columns)
					val respEntity = HttpEntity(ContentTypes.`application/json`, src)
					complete(HttpResponse(entity = respEntity))
				}
			}
		}
	}

	val standardViewerRoute = viewerRoute(new StiltResultsFetcher(config, None))

	def route: Route = pathPrefix("viewer") {
		pathPrefix("job_.+".r){ jobId =>
			val service = new StiltResultsFetcher(config, Some(jobId))
			viewerRoute(service)
		} ~
		standardViewerRoute
	} ~
	pathPrefix("worker"){
		get {
			pathEnd{redirect("worker/", StatusCodes.Found)} ~
			pathSingleSlash {
				complete(views.html.WorkerPage())
			} ~
			path("worker.js"){
				getFromResource("www/worker.js")
			} ~
			path("wsdashboardinfo"){
				handleWebSocketMessages(cluster.websocketsFlow)
			}
		} ~
		post{
			path("enqueuejob"){
				user{userId =>
					entity(as[Job]){job =>
						if(job.userId == userId.email){
							cluster.addJob(job)
							complete(StatusCodes.OK)
						}else{
							complete((StatusCodes.Forbidden, "Wrong user id in the job definition!"))
						}
					} ~
					complete((StatusCodes.BadRequest, "Wrong request payload, expected a proper Job object"))
				} ~
				complete((StatusCodes.Forbidden, "Please log in with Carbon Portal"))
			}
		} ~
		post {
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
		path("whoami"){
			user{userId =>
				complete((StatusCodes.OK,
						  WhoamiResult(userId.email, config.admins.exists(_ == userId.email))))
			} ~
			complete((StatusCodes.OK, WhoamiResult("")))
		}
	}

}
