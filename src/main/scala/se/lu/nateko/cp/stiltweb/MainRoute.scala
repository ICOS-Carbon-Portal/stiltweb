package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.sys.process._
import scala.io.Source
import spray.json._
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.stiltweb.services.StiltResultsFetcher

case class StiltResultsRequest(stationId: String, year: Int, columns: Seq[String])

case class Columns(data: Array[String])

case class DataLoad(labels: Array[String], dates: Array[Array[String]], data: Array[Array[Double]])

case class Site(site: String)

object StiltWebJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val columnsFormat = jsonFormat1(Columns)
  implicit val dataLoadFormat = jsonFormat3(DataLoad)
  implicit val siteFormat = jsonFormat1(Site)
  implicit val stiltResultsRequestFormat = jsonFormat3(StiltResultsRequest)
}

class MainRoute(config: StiltWebConfig) extends DefaultJsonProtocol {

  import StiltWebJsonSupport._

  private val service = new StiltResultsFetcher(config)

  def runStilt(ds: String): Int = Process(s"/opt/STILT_modelling/start.stilt.sh $ds").!

  private implicit val rasterMarshalling = RasterMarshalling.marshaller
  private implicit val templateMarshaller = TemplatePageMarshalling.marshaller

  private val mainPage = complete(views.html.StiltPage())
  private val workerPage = complete(views.html.WorkerPage())
  private val viewerPage = complete(views.html.ViewerPage())

  def route: Route =
	pathSingleSlash {
	  mainPage
	} ~
	pathPrefix("worker"){
	  pathSingleSlash {
		workerPage
	  } ~
	  path("worker.js"){
		getFromResource("www/worker.js")
	  } ~
	  get {
		path("stationyears") {
		  complete(service.getStationsAndYears)
		} ~
		path("listfootprints") {
		  parameters("stationId", "year".as[Int]) { (stationId, year) =>
			complete(service.getFootprintFiles(stationId, year))
		  }
		}
	  }
	} ~
	pathPrefix("viewer") {
	  pathSingleSlash {
		viewerPage
	  } ~
	  path("viewer.js") {
		getFromResource("www/viewer.js")
	  } ~
	  get {
		path("stationyears") {
		  complete(service.getStationsAndYears)
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
		} ~
		formFields('dataset) { dataset =>
		  val retCode: Int = runStilt(dataset)
		  if (retCode == 0) {
			redirect("/showdata", StatusCodes.Found)
		  } else {
			getFromResource("www/stiltfailure.html")
		  }
		}
	  }
	} ~
	pathPrefix("getData") {
	  post {
		entity(as[Columns]) { columns =>
		  val lines = Source.fromFile(config.pathToMockData).getLines().toStream
		  val headerCells = lines.head.split(' ')
		  val colIndicies: Array[Int] = columns.data.map(headerCells.indexOf)

		  val dates: Stream[Array[String]] = lines.tail.map { line =>
			val cells: Array[String] = line.split(' ')
			Array(0).map(idx => cells(idx))
		  }

		  val values: Stream[Array[Double]] = lines.tail.map { line =>
			val cells: Array[String] = line.split(' ')
			colIndicies.map(idx => cells(idx).toDouble)
		  }

		  complete(new DataLoad(columns.data, dates.toArray, values.toArray).toJson(dataLoadFormat))

		} ~ {
		  complete((StatusCodes.BadRequest, "Expected a JSON object with 'data' string array"))
		}
	  }
	} ~
	pathPrefix("startStilt") {
	  post {
		entity(as[Site]) { site =>
		  //Working with a pre-computed file. STILT should be started here just as in the stilt route.
		  complete("Working on " + site.site)
		}
	  }
	}
}
