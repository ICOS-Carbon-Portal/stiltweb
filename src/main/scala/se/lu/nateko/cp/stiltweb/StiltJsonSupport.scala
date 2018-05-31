package se.lu.nateko.cp.stiltweb

import java.time.{ Instant, LocalDate }

import akka.actor.Address
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.stiltcluster.{DashboardInfo, Job, JobInfo, StiltPosition, StiltSlot, StiltTime, WorkMasterStatus, WorkerNodeInfo}
import spray.json._

object StiltJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

	implicit val rasterMarshalling = RasterMarshalling.marshaller
	implicit val templateMarshaller = TemplatePageMarshalling.marshaller

	implicit val stiltResultsWhoamiFormat = jsonFormat2(WhoamiResult)
	implicit val stiltResultsRequestFormat = jsonFormat3(StiltResultsRequest)
	implicit val stiltStationInfoFormat = jsonFormat7(StiltStationInfo)
	implicit object LocalDateFormat extends JsonFormat[LocalDate]{
		def write(d: LocalDate) = JsString(d.toString)
		def read(value: JsValue) = value match {
			case JsString(d) => LocalDate.parse(d)
			case _ => throw new DeserializationException("Expected a date string in format YYYY-MM-DD")
		}
	}

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

	implicit val jobInfoFormat = jsonFormat3(JobInfo)
	implicit val workMasterStatusFormat = jsonFormat2(WorkMasterStatus)
	implicit val workerNodeInfoFormat = jsonFormat3(WorkerNodeInfo)

	implicit val dashboardInfoFormat = jsonFormat4(DashboardInfo)

	implicit val stiltTimeFormat = jsonFormat4(StiltTime.apply)
	implicit val stiltPositionFormat = jsonFormat3(StiltPosition.apply)
	implicit val stiltSlotFormat = jsonFormat2(StiltSlot.apply)
}
