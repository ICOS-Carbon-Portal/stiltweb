package se.lu.nateko.cp.stiltweb

import scala.io.Source
import StiltStationIds.*
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import se.lu.nateko.cp.stiltcluster.Job

object StationsFileDriver:
	type IdInfo = Map[StiltId, StiltStationIds]

	private val stagingFilePath: Path = Paths.get("./stationsStaging.csv")
	private def stockFileSrc = Source
		.fromInputStream(getClass.getResourceAsStream("/stations.csv"), "UTF-8")

	def readInfo: IdInfo = readStaging ++ parse(stockFileSrc)

	def updateStaging(job: Job): Unit =
		val current = readStaging
		val jobIds = StiltStationIds(
			id = job.siteId,
			name = job.siteName,
			icosId = job.icosId,
			icosHeight = None,
			countryCode = job.countryCode
		)
		val updated = current + (job.siteId -> jobIds)
		writeStaging(updated)

	private def writeStaging(info: IdInfo): Unit =
		val header = Seq(STILT_id, STILT_name, ICOS_id, ICOS_height, Country).mkString(",")
		val lines = LazyList(header) ++ info.valuesIterator.map: id =>
			val cells = id.id +:
				Seq(id.name, id.icosId, id.icosHeight, id.countryCode).map(_.getOrElse(""))
			cells.mkString(",")

		Files.writeString(stagingFilePath, lines.mkString("\n"))

	private def readStaging: IdInfo =
		if Files.exists(stagingFilePath) then
			val src = Source.fromFile(stagingFilePath.toFile)
			parse(src)
		else Map.empty

	private def parse(src: Source): IdInfo =
		try
			val lines = src.getLines()

			val headerIdxs: Map[String, Int] = lines.next().split(",", -1).map(_.trim).zipWithIndex.toMap

			lines.map: line =>
				val cells = line.split(",", -1).map(_.trim)
				def cell(colName: String) = cells(headerIdxs(colName))
				val id = cell(STILT_id)
				val Array(name, icosId, icosHeight, ccode) = Array(STILT_name, ICOS_id, ICOS_height, Country)
					.map(s => Option(cell(s)).filterNot(_.isEmpty))
				id -> StiltStationIds(id, name, icosId, icosHeight.flatMap(_.toFloatOption), ccode)
			.toMap
		finally
			src.close()
	end parse

end StationsFileDriver
