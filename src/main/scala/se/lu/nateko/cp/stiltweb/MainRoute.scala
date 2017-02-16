package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import se.lu.nateko.cp.stiltcluster.Job

class MainRoute(service: StiltResultsFetcher, cluster: StiltClusterApi) {

	import StiltJsonSupport._

	def route: Route = pathPrefix("viewer") {
		get {
			pathEnd{redirect("viewer/", StatusCodes.Found)} ~
			pathSingleSlash {
				complete(views.html.ViewerPage())
			} ~
			path("viewer.js") {
				getFromResource("www/viewer.js")
			} ~
			path("listfootprints") {
				parameters("stationId", "year".as[Int]) { (stationId, year) =>
					complete(service.getFootprintFiles(stationId, year))
				}
			} ~
			path("footprint") {
				parameters("stationId", "footprint") { (stationId, filename) =>
					complete(service.getFootprintRaster(stationId, filename))
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
				entity(as[Job]){job =>
					cluster.addJob(job)
					complete(StatusCodes.OK)
				} ~
				complete((StatusCodes.BadRequest, "Wrong request payload, expected a proper Job object"))
			}
		}
	} ~
	get{
		path("stationinfo") {
			complete(service.getStationInfos)
		} ~
		pathEndOrSingleSlash{
			redirect("/viewer/", StatusCodes.Found)
		}
	}

}
