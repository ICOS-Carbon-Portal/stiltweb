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

	implicit val rasterMarshalling: ToResponseMarshaller[Raster] = RasterMarshalling.marshaller
	implicit val templateMarshaller: ToResponseMarshaller[Html] = TemplatePageMarshalling.marshaller

	implicit val stiltResultsWhoamiFormat: JsonFormat[WhoamiResult] = jsonFormat2(WhoamiResult.apply)

	implicit object stiltStationInfoWriter extends RootJsonFormat[StiltStationInfo] {
		private implicit val idsformat: JsonFormat[StiltStationIds] = jsonFormat5(StiltStationIds.apply)
		private val simple: JsonFormat[StiltStationInfo] = jsonFormat5(StiltStationInfo.apply)

		def write(si: StiltStationInfo): JsValue = {
			val self = si.toJson(simple).asJsObject
			val ids = si.id.toJson.asJsObject
			JsObject(self.fields ++ ids.fields)
		}

		def read(json: JsValue): StiltStationInfo = ???
	}

	implicit object LocalDateFormat extends JsonFormat[LocalDate]{
		def write(d: LocalDate) = JsString(d.toString)
		def read(value: JsValue) = value match {
			case JsString(d) => LocalDate.parse(d)
			case _ => throw new DeserializationException("Expected a date string in format YYYY-MM-DD")
		}
	}
	implicit val stiltResultsRequestFormat: JsonFormat[StiltResultsRequest] = jsonFormat4(StiltResultsRequest.apply)

	implicit object ActorAddressFormat extends JsonFormat[Address]{
		def write(a: Address) = JsString(a.toString)
		def read(value: JsValue) = throw new DeserializationException(
			"JSON-parsing of akka.actor.Address instances is not implemented and should not be needed"
		)
	}

	implicit object InstantFormat extends JsonFormat[Instant]{
		def write(i: Instant) = JsString(i.toString)
		// We never want to read the time{Started,Stopped} from JSON
		def read(value: JsValue) = throw new DeserializationException(
			"JSON-parsing of Instant is not needed/accepted."
		)
	}

	private val jobDefaultFormat: JsonFormat[Job] = jsonFormat9(Job.apply)

	implicit object JobFormat extends RootJsonFormat[Job]{
		def write(job: Job) = {
			val basic = jobDefaultFormat.write(job).asJsObject
			JsObject(basic.fields.filterNot{
						 t => t._1 == "timeStarted" || t._1 == "timeStopped"}
						 + ("id" -> JsString(job.id)))
		}
		def read(value: JsValue) = jobDefaultFormat.read(value)
	}

	implicit val stiltTimeFormat: JsonFormat[StiltTime] = jsonFormat4(StiltTime.apply)
	implicit val stiltPositionFormat: JsonFormat[StiltPosition] = jsonFormat3(StiltPosition.apply)
	implicit val stiltSlotFormat: JsonFormat[StiltSlot] = jsonFormat2(StiltSlot.apply)
	implicit val slotFailureFormat: JsonFormat[SlotFailure] = jsonFormat3(SlotFailure.apply)

	implicit val jobInfoFormat: JsonFormat[JobInfo] = jsonFormat4(JobInfo.apply)
	implicit val workMasterStatusFormat: JsonFormat[WorkMasterStatus] = jsonFormat3(WorkMasterStatus.apply)
	implicit val workerNodeInfoFormat: JsonFormat[WorkerNodeInfo] = jsonFormat3(WorkerNodeInfo.apply)

	implicit val dashboardInfoFormat: JsonFormat[DashboardInfo] = jsonFormat4(DashboardInfo.apply)


}
