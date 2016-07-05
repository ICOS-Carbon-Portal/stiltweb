package se.lu.nateko.cp.stiltweb

import java.util
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import scala.sys.process._
import scala.io.Source
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

case class Columns(data: Array[String])

case class DataLoad(labels: Array[String], dates: Array[Array[String]], data: Array[Array[Double]])

case class Site(site: String)

object StiltwebJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
	implicit val columnsFormat = jsonFormat1(Columns)
	implicit val dataLoadFormat = jsonFormat3(DataLoad)
	implicit val siteFormat = jsonFormat1(Site)
}

class MainRoute(config: StiltWebConfig) {

	def runStilt(ds: String): Int = Process(s"/opt/STILT_modelling/start.stilt.sh $ds").!

	private val mainPage = getFromResource("www/stiltReact.html")

	def route: Route =
		pathSingleSlash { mainPage } ~
		path("stilt.js"){
			getFromResource("www/stilt.js")
		} ~
		pathPrefix("stilt") {
			pathEndOrSingleSlash { mainPage } ~
			post {
				formFields('dataset) { dataset => val retCode: Int = runStilt(dataset)
					if (retCode == 0){
						redirect("/showdata", StatusCodes.Found)
					}else{
						getFromResource("www/stiltfailure.html")
					}
				}
			}
		} ~
		pathPrefix("showdata"){ mainPage } ~
		pathPrefix("getData"){
			post {
				import StiltwebJsonSupport.columnsFormat
				import StiltwebJsonSupport.dataLoadFormat

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
		pathPrefix("startStilt"){
			post {
				import StiltwebJsonSupport.siteFormat
				entity(as[Site]) { site =>
						//Working with a pre-computed file. STILT should be started here just as in the stilt route.
					complete("Working so far")
				}
			}
		}

	}
