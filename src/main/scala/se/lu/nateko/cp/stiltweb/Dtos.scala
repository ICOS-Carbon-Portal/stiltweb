package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

case class StiltResultsRequest(
	stationId: String,
	columns: Seq[String],
	fromDate: LocalDate,
	toDate: LocalDate
)

case class StiltStationIds(
	id: String,
	name: Option[String],
	icosId: Option[String],
	wdcggId: Option[String],
	globalviewId: Option[String]
)

object StiltStationIds{
	def apply(id: String): StiltStationIds = StiltStationIds(id, None, None, None, None)
}

case class StiltStationInfo(id: StiltStationIds, lat: Double, lon: Double, alt: Int, years: Seq[Int])

case class WhoamiResult(email: String, isAdmin: Boolean = false)
