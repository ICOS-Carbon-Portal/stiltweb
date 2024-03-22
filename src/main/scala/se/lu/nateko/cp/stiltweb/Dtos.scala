package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

case class StiltResultsRequest(
	stationId: String,
	columns: Option[Seq[String]],
	fromDate: LocalDate,
	toDate: LocalDate
):
	def batch = ResultBatch(stationId, fromDate, toDate)

case class StiltStationIds(
	id: String,
	name: Option[String],
	icosId: Option[String],
	icosHeight: Option[Float],
	countryCode: Option[String]
)

object StiltStationIds{
	val STILT_id = "STILT id"
	val STILT_name = "STILT name"
	val ICOS_id = "ICOS id"
	val ICOS_height = "ICOS height"
	val Country = "Country"
	def apply(id: String): StiltStationIds = StiltStationIds(id, None, None, None, None)
}

case class StiltStationInfo(id: StiltStationIds, lat: Double, lon: Double, alt: Int, years: Seq[Int])

case class WhoamiResult(email: String, isAdmin: Boolean = false)
