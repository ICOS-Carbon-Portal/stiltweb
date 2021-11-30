package se.lu.nateko.cp.stiltweb.marshalling

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling.WithOpenCharset
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import se.lu.nateko.cp.stiltweb.StiltStationInfo
import scala.concurrent.Future
import akka.http.scaladsl.model.ContentType.WithCharset
import spray.json._

object StationInfoMarshalling{
	type Stations = Seq[StiltStationInfo]
	import StiltJsonSupport._

	val jsonMarshaller: ToResponseMarshaller[Stations] =
		Marshaller.liftMarshaller(summon[RootJsonWriter[Stations]])

	val csvMarshaller: ToResponseMarshaller[Stations] = Marshaller(
		_ => stations => Future.successful(
			List(MediaTypes.`text/plain`, MediaTypes.`text/csv`).map{media =>
				WithOpenCharset(media, charset => getText(stations, WithCharset(media, charset)))
			}
		)
	)

	implicit val stationInfoMarshaller: ToResponseMarshaller[Stations] = Marshaller.oneOf(csvMarshaller, jsonMarshaller)

	private def getText(stations: Stations, contentType: WithCharset): HttpResponse = {
		val lines: Seq[String] = "STILT id,STILT name,ICOS id,WDCGG,GLOBALVIEW" +: stations.map{station =>
			import station.id._
			val cells = id +: Seq(name, icosId, wdcggId, globalviewId).map(_.getOrElse(""))
			cells.mkString(",")
		}
		val bytes = ByteString(lines.mkString("\n"), contentType.charset.value)
		HttpResponse(entity = HttpEntity(contentType, Source.single(bytes)))
	}
}
