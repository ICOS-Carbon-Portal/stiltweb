package se.lu.nateko.cp.stiltweb.marshalling

import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.marshalling.Marshalling.WithOpenCharset
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshaller
import org.apache.pekko.http.scaladsl.marshalling.ToEntityMarshaller
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import se.lu.nateko.cp.stiltweb.StiltStationInfo
import scala.concurrent.Future
import org.apache.pekko.http.scaladsl.model.ContentType.WithCharset
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

object StationInfoMarshalling{
	type Stations = Seq[StiltStationInfo]

	val jsonMarshaller: ToResponseMarshaller[Stations] = {
		import StiltJsonSupport.{given JsonFormat[StiltStationInfo]}
		import DefaultJsonProtocol.immSeqFormat
		given [T: RootJsonWriter]: ToEntityMarshaller[T] = SprayJsonSupport.sprayJsonMarshaller
		summon[ToEntityMarshaller[Stations]]
	}

	val csvMarshaller: ToResponseMarshaller[Stations] = Marshaller(
		_ => stations => Future.successful(
			List(MediaTypes.`text/plain`, MediaTypes.`text/csv`).map{media =>
				WithOpenCharset(media, charset => getText(stations, WithCharset(media, charset)))
			}
		)
	)

	given ToResponseMarshaller[Stations] = Marshaller.oneOf(csvMarshaller, jsonMarshaller)

	private def getText(stations: Stations, contentType: WithCharset): HttpResponse = {
		import se.lu.nateko.cp.stiltweb.StiltStationIds.{STILT_id, STILT_name, ICOS_id, ICOS_height, Country}

		val lines: Seq[String] = Array(
			STILT_id, STILT_name, ICOS_id, ICOS_height, Country, "STILT lat", "STILT lon", "STILT alt"
		).mkString(",") +: stations.map{station =>
			import station.id
			val cells = id.id +:
				Seq(id.name, id.icosId, id.icosHeight, id.countryCode).map(_.getOrElse("")) ++:
				Seq(station.lat, station.lon, station.alt)
			cells.mkString(",")
		}
		val bytes = ByteString(lines.mkString("\n"), contentType.charset.value)
		HttpResponse(entity = HttpEntity(contentType, Source.single(bytes)))

	}
}
