package se.lu.nateko.cp.stiltweb.marshalling

import java.time.{ Instant, LocalDate }
import akka.actor.Address
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.data.formats.netcdf.viewing.Raster
import se.lu.nateko.cp.stiltcluster._
import spray.json._
import se.lu.nateko.cp.stiltweb.StiltResultsRequest
import se.lu.nateko.cp.stiltweb.StiltStationInfo
import se.lu.nateko.cp.stiltweb.WhoamiResult
import se.lu.nateko.cp.stiltweb.StiltStationIds
import play.twirl.api.Html

object StiltJsonSupport {
	import SprayJsonSupport._
	import DefaultJsonProtocol._

	given ToResponseMarshaller[Raster] = RasterMarshalling.marshaller
	given ToResponseMarshaller[Html] = TemplatePageMarshalling.marshaller

	given RootJsonFormat[WhoamiResult] = jsonFormat2(WhoamiResult.apply)

	given RootJsonFormat[StiltStationInfo] with {
		private given JsonFormat[StiltStationIds] = jsonFormat4(StiltStationIds.apply)
		private val simple: JsonFormat[StiltStationInfo] = jsonFormat5(StiltStationInfo.apply)

		def write(si: StiltStationInfo): JsValue = {
			val self = si.toJson(simple).asJsObject
			val ids = si.id.toJson.asJsObject
			JsObject(self.fields ++ ids.fields)
		}

		def read(json: JsValue): StiltStationInfo = ???
	}

	given JsonFormat[LocalDate] with{
		def write(d: LocalDate) = JsString(d.toString)
		def read(value: JsValue) = value match {
			case JsString(d) => LocalDate.parse(d)
			case _ => throw new DeserializationException("Expected a date string in format YYYY-MM-DD")
		}
	}
	given RootJsonFormat[StiltResultsRequest] = jsonFormat4(StiltResultsRequest.apply)

	given JsonFormat[Address] with {
		def write(a: Address) = JsString(a.toString)
		def read(value: JsValue) = throw new DeserializationException(
			"JSON-parsing of akka.actor.Address instances is not implemented and should not be needed"
		)
	}

	given JsonFormat[Instant] with {
		def write(i: Instant) = JsString(i.toString)
		// We never want to read the time{Started,Stopped} from JSON
		def read(value: JsValue) = throw new DeserializationException(
			"JSON-parsing of Instant is not needed/accepted."
		)
	}

	private val jobDefaultFormat: JsonFormat[Job] = jsonFormat9(Job.apply)

	given RootJsonFormat[Job] with {
		def write(job: Job) = {
			val basic = jobDefaultFormat.write(job).asJsObject
			JsObject(basic.fields.filterNot{
						 t => t._1 == "timeStarted" || t._1 == "timeStopped"}
						 + ("id" -> JsString(job.id)))
		}
		def read(value: JsValue) = jobDefaultFormat.read(value)
	}

	given JsonFormat[StiltTime] = jsonFormat4(StiltTime.apply)
	given JsonFormat[StiltPosition] = jsonFormat3(StiltPosition.apply)
	given JsonFormat[StiltSlot] = jsonFormat2(StiltSlot.apply)
	given JsonFormat[SlotFailure] = jsonFormat3(SlotFailure.apply)

	given JsonFormat[JobInfo] = jsonFormat4(JobInfo.apply)
	given JsonFormat[WorkMasterStatus] = jsonFormat3(WorkMasterStatus.apply)
	given JsonFormat[WorkerNodeInfo] = jsonFormat3(WorkerNodeInfo.apply)

	given JsonFormat[DashboardInfo] = jsonFormat4(DashboardInfo.apply)


}
