package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

case class StiltResultsRequest(
	stationId: String,
	columns: Seq[String],
	fromDate: LocalDate,
	toDate: LocalDate
)

case class StiltStationInfo(
	id: String,
	name: Option[String],
	lat: Double, lon: Double, alt: Int,
	years: Seq[Int],
	wdcggId: Option[String]
)
