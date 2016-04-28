package se.lu.nateko.cp.stiltweb

import java.util
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import scala.sys.process._
import scala.io.Source
import spray.json._


class MainRoute(config: StiltWebConfig) {

	def runStilt(ds: String): Int = Process(s"/opt/STILT_modelling/start.stilt.sh $ds").!

	def route: Route =
		pathSingleSlash {
			getFromResource("www/stiltinput.html")
		} ~
		pathPrefix("stilt") {
			pathSingleSlash {
				getFromResource("www/stiltinput.html")
			} ~
			post {
				formFields('dataset) { dataset => val retCode: Int = runStilt(dataset)
					if (retCode == 0){
						getFromResource("www/stiltsuccess.html")
					}else{
						getFromResource("www/stiltfailure.html")
					}
				}
			}
		} ~
		pathPrefix("success"){
			//getFromResource("www/stiltsuccess.html")
			val lines = Source.fromFile("/opt/stiltresult1.csv").getLines().toStream
			val headerCells = lines.head.split(' ')
			val colIndicies: Array[Int] = Array("\"co2ffm\"", "\"co2ini\"").map(headerCells.indexOf)

			val values: Stream[Array[String]] = lines.tail.map { line =>
				val cells: Array[String] = line.split(' ')
				colIndicies.map(idx => cells(idx))
			}
			var body = headerCells(colIndicies(0)) + "\t\t" + headerCells(colIndicies(1)) + "\n"
			body += values.map(row => row.mkString(" \t")).mkString("\n")
			complete(body)
		}
	}

