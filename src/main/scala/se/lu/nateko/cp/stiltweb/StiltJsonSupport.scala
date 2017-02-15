package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

import akka.actor.Address
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import se.lu.nateko.cp.data.formats.netcdf.RasterMarshalling
import se.lu.nateko.cp.stiltcluster.DashboardInfo
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.JobInfo
import se.lu.nateko.cp.stiltcluster.JobRun
import se.lu.nateko.cp.stiltcluster.JobStatus
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat

object StiltJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

	implicit val rasterMarshalling = RasterMarshalling.marshaller
	implicit val templateMarshaller = TemplatePageMarshalling.marshaller

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

	implicit val jobFormat = jsonFormat6(Job)
	implicit val jobRunFormat = jsonFormat2(JobRun)
	implicit val jobStatusFormat = jsonFormat5(JobStatus.apply)
	implicit val jobInfoFormat = jsonFormat3(JobInfo)
	implicit val dashboardInfoWriter = jsonFormat3(DashboardInfo)
}
