package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

import akka.actor.Address
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.stiltcluster.DashboardInfo
import se.lu.nateko.cp.stiltcluster.LogEntry
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.JobInfo
import se.lu.nateko.cp.stiltcluster.JobRun
import se.lu.nateko.cp.stiltcluster.JobStatus
import scala.collection.mutable.ListBuffer
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat,JsArray,deserializationError}
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

	implicit val LogEntryFormat = jsonFormat2(LogEntry)
	private val jobDefaultFormat = jsonFormat8(Job)

	implicit def listBufferFormat[T :JsonFormat] = new RootJsonFormat[ListBuffer[T]] {
		def write(listBuffer: ListBuffer[T]) = JsArray(listBuffer.map(_.toJson).toVector)
		def read(value: JsValue): ListBuffer[T] = value match {
			case JsArray(elements) => elements.map(_.convertTo[T])(collection.breakOut)
			case x => deserializationError("Expected ListBuffer as JsArray, but got " + x)
		}
	}

	implicit object JobFormat extends RootJsonFormat[Job]{
		def write(job: Job) = {
			val basic = jobDefaultFormat.write(job).asJsObject
			JsObject(basic.fields + ("id" -> JsString(job.id)))
		}
		def read(value: JsValue) = jobDefaultFormat.read(value)
	}

	implicit val jobRunFormat = jsonFormat2(JobRun)
	implicit val jobStatusFormat = jsonFormat5(JobStatus.apply)
	implicit val jobInfoFormat = jsonFormat3(JobInfo)
	implicit val dashboardInfoFormat = jsonFormat3(DashboardInfo)
}
