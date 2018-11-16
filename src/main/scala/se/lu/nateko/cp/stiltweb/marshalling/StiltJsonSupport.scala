package se.lu.nateko.cp.stiltweb.marshalling

import java.time.{ Instant, LocalDate }
import akka.actor.Address
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.stiltcluster._
import spray.json._
import se.lu.nateko.cp.stiltweb.StiltResultsRequest
import se.lu.nateko.cp.stiltweb.StiltStationInfo
import se.lu.nateko.cp.stiltweb.WhoamiResult
import se.lu.nateko.cp.stiltweb.StiltStationIds

object StiltJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

	implicit val rasterMarshalling = RasterMarshalling.marshaller
	implicit val templateMarshaller = TemplatePageMarshalling.marshaller

	implicit val stiltResultsWhoamiFormat = jsonFormat2(WhoamiResult)

	implicit object stiltStationInfoWriter extends RootJsonFormat[StiltStationInfo] {
		private implicit val idsformat = jsonFormat5(StiltStationIds.apply)
		private val simple = jsonFormat5(StiltStationInfo)

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
	implicit val stiltResultsRequestFormat = jsonFormat4(StiltResultsRequest)

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

	private val jobDefaultFormat = jsonFormat9(Job)

	implicit object JobFormat extends RootJsonFormat[Job]{
		def write(job: Job) = {
			val basic = jobDefaultFormat.write(job).asJsObject
			JsObject(basic.fields.filterNot{
						 t => t._1 == "timeStarted" || t._1 == "timeStopped"}
						 + ("id" -> JsString(job.id)))
		}
		def read(value: JsValue) = jobDefaultFormat.read(value)
	}

	implicit val stiltTimeFormat = jsonFormat4(StiltTime.apply)
	implicit val stiltPositionFormat = jsonFormat3(StiltPosition.apply)
	implicit val stiltSlotFormat = jsonFormat2(StiltSlot.apply)
	implicit val slotFailureFormat = jsonFormat3(SlotFailure)

	implicit val jobInfoFormat = jsonFormat4(JobInfo)
	implicit val workMasterStatusFormat = jsonFormat3(WorkMasterStatus)
	implicit val workerNodeInfoFormat = jsonFormat3(WorkerNodeInfo)

	implicit val dashboardInfoFormat = jsonFormat4(DashboardInfo)


}
