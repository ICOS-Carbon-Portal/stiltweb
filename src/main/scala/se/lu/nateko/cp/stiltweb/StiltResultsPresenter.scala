package se.lu.nateko.cp.stiltweb

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.Year
import java.time.ZoneOffset

import scala.jdk.CollectionConverters._
import scala.io.{ Source => IoSource }
import scala.util.Try

import se.lu.nateko.cp.data.formats.netcdf.viewing.Raster
import se.lu.nateko.cp.data.formats.netcdf.viewing.impl.ViewServiceFactoryImpl
import se.lu.nateko.cp.stiltcluster.StiltPosition
import se.lu.nateko.cp.stiltcluster.StiltResultFileType
import se.lu.nateko.cp.stiltcluster.StiltTime
import se.lu.nateko.cp.stiltweb.csv.LocalDayTime
import se.lu.nateko.cp.stiltweb.csv.RawRow
import se.lu.nateko.cp.stiltweb.csv.ResultRowMaker
import se.lu.nateko.cp.stiltweb.csv.RowCache
import se.lu.nateko.cp.stiltweb.netcdf.FootprintJoiner
import se.lu.nateko.cp.stiltweb.state.Archiver
import spray.json.JsNull
import spray.json.JsNumber
import spray.json.JsValue

class StiltResultsPresenter(config: StiltWebConfig) {
	import StiltResultsPresenter._

	private val archiver = new Archiver(Paths.get(config.stateDirectory), config.slotStepInMinutes)

	def getStationInfos: Seq[StiltStationInfo] = {
		import StiltStationIds.{STILT_id, STILT_name, ICOS_id, ICOS_height}

		val idToIds: Map[String, StiltStationIds] = {
			val lines = IoSource
				.fromInputStream(getClass.getResourceAsStream("/stations.csv"), "UTF-8")
				.getLines()

			val headerIdxs: Map[String, Int] = lines.next().split(",", -1).map(_.trim).zipWithIndex.toMap

			lines.map{line =>
				val cells = line.split(",", -1).map(_.trim)
				def cell(colName: String) = cells(headerIdxs(colName))
				val id = cell(STILT_id)
				val Array(name, icosId, icosHeight) = Array(STILT_name, ICOS_id, ICOS_height)
					.map(cell).map{s => if(s.isEmpty) None else Some(s)}
				id -> StiltStationIds(id, name, icosId, icosHeight.flatMap(_.toFloatOption))
			}
			.toMap
		}

		subdirectories(archiver.stationsDir).map{directory =>
			val id = directory.getFileName.toString

			val ids = idToIds.get(id).getOrElse(StiltStationIds(id))

			val pos = stiltPos(directory)

			val years = subdirectories(directory).map(_.getFileName.toString).collect{
				case yearDirPattern(dddd) => dddd.toInt
			}

			StiltStationInfo(ids, pos.lat, pos.lon, pos.alt, years)
		}
	}

	private def stiltPos(stationDir: Path): StiltPosition = {
		val id = stationDir.toRealPath().getFileName.toString
		id match{
			case StiltPosition(sp) => sp
			case _ => throw new Exception(s"Could not parse $id as stilt site id to extract lat/lon/alt")
		}
	}

	def getStiltResults(req: StiltResultsRequest): Iterator[Seq[JsValue]] = fetchStiltResults(listSlotRows, req)
	def getStiltRawResults(req: StiltResultsRequest): Iterator[Seq[JsValue]] = fetchStiltResults(listRawSlotRows, req)

	private def fetchStiltResults(fetcher: SlotCsvRowFetcher, req: StiltResultsRequest): Iterator[Seq[JsValue]] =
		reduceToSingleYearOp(fetcher(req.stationId, _, _, _))(req.fromDate, req.toDate)
			.map{ case (dt, row) =>
				req.columns
					.map{
						case "isodate" =>
							JsNumber(dt.toEpochSecond(ZoneOffset.UTC))
						case col =>
							row.get(col).fold[JsValue](JsNull)(n => JsNumber(n))
					}
			}

	def listFootprints(stationId: String, fromDate: LocalDate, toDate: LocalDate): Iterator[LocalDateTime] =
		listSlotsWithFootprints(stationId, fromDate, toDate).map(_._2)

	def mergeFootprintsToNetcdf(stationId: String, fromDate: LocalDate, toDate: LocalDate): Path = {
		val footPaths = listSlotsWithFootprints(stationId, fromDate, toDate)
			.map(_._1.resolve(StiltResultFileType.Foot.toString))
		FootprintJoiner.joinToTempFile(footPaths)
	}

	private def listSlotsWithFootprints(stationId: String, fromDate: LocalDate, toDate: LocalDate): Iterator[Slot] =
		reduceToSingleYearOp(listSlots(stationId, _, _, _))(fromDate, toDate).collect{
			case slot @ (fpDir, _) if Files.exists(fpDir.resolve(StiltResultFileType.Foot.toString)) => slot
		}

	private def yearPath(stationId: String, year: Year) = archiver.getYearDir(stationId, year.getValue)

	private def listSlotRows(stationId: String, year: Year, from: Option[MonthDay], to: Option[MonthDay]): Iterator[SlotCsvRow] = {
		val rowFactory = () => listSlots(stationId, year, None, None).flatMap{ case slot @ (_, dt) =>
			readRawRow(slot).map{rawRow =>
				LocalDayTime(dt) -> ResultRowMaker.makeRow(rawRow)
			}.toOption.iterator
		}
		val cache = new RowCache(rowFactory, yearPath(stationId, year), year.getValue, config.slotStepInMinutes)
		cache.getRows(from.map(new LocalDayTime(_, LocalTime.MIN)), to.map(new LocalDayTime(_, LocalTime.MAX)))
	}

	private def listRawSlotRows(stationId: String, year: Year, from: Option[MonthDay], to: Option[MonthDay]): Iterator[SlotCsvRow] =
		listSlots(stationId, year, from, to).flatMap{ case slot @ (_, dt) =>
			readRawRow(slot).map{rawRow =>
				dt -> rawRow.vals.map{case (v, n) => v.name -> n}
			}.toOption.iterator
		}

	private def readRawRow(slot: Slot): Try[RawRow] = Try{
		val (fpDir, _) = slot
		val fpPath = fpDir.resolve(StiltResultFileType.CSV.toString)
		val lines = Files.readAllLines(fpPath)
		RawRow.parse(lines.get(0), lines.get(1))
	}

	private def listSlots(stationId: String, yearY: Year, from: Option[MonthDay], to: Option[MonthDay]): Iterator[Slot] = {
		val year = yearY.getValue
		def toDate(md: MonthDay) = md.atYear(year)

		subdirectories(yearPath(stationId, yearY)).iterator.filter{
			val fromMonth = from.map(_.getMonthValue).getOrElse(0)
			val toMonth = to.map(_.getMonthValue).getOrElse(13)

			monthPath => monthPath.getFileName.toString match{
				case monthDirPattern(m) =>
					val mNum = m.toInt
					mNum >= fromMonth && mNum <= toMonth
				case _ => false
			}
		}
		.flatMap(subdirectories)
		.map(path => path -> path.getFileName.toString)
		.collect{
			case (path, StiltTime(stime)) => path -> stime.toJava
		}
		.filter{
			val fromDt = from.map(toDate).map(d => LocalDateTime.of(d, LocalTime.MIN)).getOrElse(LocalDateTime.of(year, 1, 1, 0, 0))
			val toDt = to.map(toDate).map(d => LocalDateTime.of(d.plusDays(1), LocalTime.MIN)).getOrElse(LocalDateTime.of(year + 1, 1, 1, 0, 0))
			slot => {
				val dt = slot._2
				dt.compareTo(fromDt) >=0 && dt.compareTo(toDt) < 0
			}
		}
		.scanLeft(new Throttler(LocalDateTime.MIN, None)){(throttler, next) =>
			val nextDt = next._2
			val maxPrevDt = nextDt.minusMinutes(config.slotStepInMinutes.toLong)
			if(throttler.lastEmitted.compareTo(maxPrevDt) <= 0)
				new Throttler(nextDt, Some(next))
			else
				new Throttler(throttler.lastEmitted, None)
		}
		.flatMap(_.toEmit)
	}

	def getFootprintRaster(stationId: String, dt: LocalDateTime): Raster = {

		val factory = {
			import config.netcdf._
			val footprintDir = archiver.getSlotDir(stationId, StiltTime.fromJava(dt)).toString + File.separator
			new ViewServiceFactoryImpl(footprintDir, dateVars.asJava, latitudeVars.asJava, longitudeVars.asJava, elevationVars.asJava)
		}
		val service = factory.getNetCdfViewService(StiltResultFileType.Foot.toString)
		val date = service.getAvailableDates()(0)
		service.getRaster(date, "foot", null)
	}

	/** List the months for which available meteorology input data is available.
	  *
	  * To start the actual STILT simulations we need input data. That data is
	  * in the form of meteorology data and resides on local disk, in the
	  * directory named by the `metDataDir' config value. This method is called
	  * (through the JSON API) by the web frontend to restrict the calendar used
	  * to select date ranges for jobs.
	  *
	  * The directory on disk will have files looking like this:
	  *   /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12010100.IN
	  *   /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12110100.arl
	  *   /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12030100.arl
	  *   /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12040100.IN
	  *   /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12090100.arl
	  *
	  * The .arl files contain the meteorology data. Given the above list of files we return
	  *   Seq("2012-01", "2012-11", "2012-03", "2012-04", "2012-09")
	  */
	def availableInputMonths(): Seq[String] = {
		val pat = "\\w+\\.(\\d\\d)(\\d\\d)0100\\.arl".r
		listFileNames(Paths.get(config.metDataDirectory), "*.arl")
			.collect{
				// "ECmetF.12090100.arl" => (12, 09) => "2012-09"
				case pat(year, month) => s"20${year}-${month}"
			}
			.distinct
			.sorted
	}
}

object StiltResultsPresenter{
	type Slot = (Path, LocalDateTime)
	type CsvRow = Map[String, Double]
	type SlotCsvRow = (LocalDateTime, CsvRow)
	type SlotCsvRowFetcher = (String, Year, Option[MonthDay], Option[MonthDay]) => Iterator[SlotCsvRow]

	// ex: 2007
	val yearDirPattern = """^(\d{4})$""".r
	// ex: 07, 5
	val monthDirPattern = """^(\d\d?)$""".r

	def subdirectories(dir: Path): IndexedSeq[Path] = {
		if(!Files.exists(dir) || !Files.isDirectory(dir)) IndexedSeq.empty else {
			val ds = Files.newDirectoryStream(dir, p => Files.isDirectory(p))
			try{
				ds.iterator.asScala.toIndexedSeq.sortBy(_.getFileName)
			} finally{
				ds.close()
			}
		}
	}

	def listFileNames(dir: Path, fileGlob: String, limit: Option[Int] = None): IndexedSeq[String] =
		listFiles(dir, fileGlob, limit).map(_.getFileName.toString)

	def listFiles(dir: Path, fileGlob: String, limit: Option[Int] = None): IndexedSeq[Path] = {
		val dirStream = Files.newDirectoryStream(dir, fileGlob)
		try{
			val fnameIter = dirStream.iterator().asScala
			(limit match{
				case None => fnameIter
				case Some(lim) => fnameIter.take(lim)
			}).toIndexedSeq
		} finally {
			dirStream.close()
		}
	}

	def reduceToSingleYearOp[T](
		singleYearOp: (Year, Option[MonthDay], Option[MonthDay]) => Iterator[T]
	)(fromDate: LocalDate, toDate: LocalDate): Iterator[T] = {

		if(fromDate.compareTo(toDate) > 0) Iterator.empty else{

			val fromMonthDay = Some(MonthDay.from(fromDate))
			val toMonthDay = Some(MonthDay.from(toDate))

			fromDate.getYear.to(toDate.getYear).distinct.map(Year.of).toList match{

				case year :: Nil =>
					singleYearOp(year, fromMonthDay, toMonthDay)

				case year1 :: year2 :: Nil =>
					singleYearOp(year1, fromMonthDay, None) ++
					singleYearOp(year2, None, toMonthDay)

				case years =>
					singleYearOp(years.head, fromMonthDay, None) ++
					years.tail.dropRight(1).iterator.flatMap(singleYearOp(_, None, None)) ++
					singleYearOp(years.last, None, toMonthDay)
			}
		}
	}

	private class Throttler(val lastEmitted: LocalDateTime, val toEmit: Option[Slot])

}
