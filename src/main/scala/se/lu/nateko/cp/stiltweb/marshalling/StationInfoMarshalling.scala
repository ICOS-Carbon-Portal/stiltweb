package se.lu.nateko.cp.stiltweb.marshalling

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling.WithOpenCharset
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import se.lu.nateko.cp.stiltweb.StiltStationInfo
import scala.concurrent.Future
import scala.io.{ Source => IoSource }
import scala.util.Using
import akka.http.scaladsl.model.ContentType.WithCharset
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

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

		val csvStr = Using(IoSource.fromInputStream(getClass.getResourceAsStream("/stations.csv"), "UTF-8")){
			src => src.getLines().mkString("\n")
		}

		HttpResponse(entity = HttpEntity(contentType, csvStr.get))
	}
}
