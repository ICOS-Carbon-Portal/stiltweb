package se.lu.nateko.cp.stiltweb

case class StiltResultsRequest(stationId: String, year: Int, columns: Seq[String])

case class StiltStationInfo(
	id: String,
	lat: Double, lon: Double, alt: Int,
	years: Seq[Int],
	icosId: Option[String],
	wdcggId: Option[String]
)
