package se.lu.nateko.cp.stiltweb

import java.nio.file.Paths
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import scala.collection.JavaConverters._
import scala.collection.parallel.availableProcessors
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.NotUsed
import se.lu.nateko.cp.data.formats.netcdf.viewing.Raster
import se.lu.nateko.cp.data.formats.netcdf.viewing.impl.ViewServiceFactoryImpl
import se.lu.nateko.cp.stiltweb.csv.RawRow
import se.lu.nateko.cp.stiltweb.csv.ResultRowMaker


class StiltResultsPresenter(config: StiltWebConfig) {
	import StiltResultFetcher._

	private val stationsDir = Paths.get(config.stateDirectory, stationsDirectory)

	def getStationInfos: Seq[StiltStationInfo] = {
		val stiltToIcos: Map[String, String] = config.stations.collect{
			case Seq(stilt, icos, _) if !icos.isEmpty => (stilt, icos)
		}.toMap

		val stiltToWdcgg: Map[String, String] = config.stations.collect{
			case Seq(stilt, _, wdcgg) if !wdcgg.isEmpty => (stilt, wdcgg)
		}.toMap

		val stiltToYears: Map[String, Seq[Int]] = getStationYears

		val stationDirectories = subdirectories(stationsDir)

		stationDirectories.map{directory =>
			val stiltId = directory.getFileName.toString
			val (lat, lon, alt) = latLonAlt(directory)
			val years = stiltToYears.get(stiltId).getOrElse(Nil)
			StiltStationInfo(stiltId, lat, lon, alt, years, stiltToIcos.get(stiltId), stiltToWdcgg.get(stiltId))
		}
	}

	private def stationYears(dir: Path): Seq[Int] = subdirectories(dir).map(_.getFileName.toString).collect{
		case yearDirPattern(dddd) => dddd.toInt
	}

	private def getStationYears: Map[String, Seq[Int]] = {
		val stationDirs = subdirectories(stationsDir)
		stationDirs.map(stDir => (stDir.getFileName.toString, stationYears(stDir))).toMap
	}

	private def latLonAlt(stationDir: Path): (Double, Double, Int) = {
		val id = stationDir.toRealPath().getFileName.toString
		try{
			id match{

				case siteIdRegex(latStr, latSignStr, lonStr, lonSignStr, altStr) =>

					val lat = latStr.toDouble * (if(latSignStr == "N") 1 else -1)
					val lon = lonStr.toDouble * (if(lonSignStr == "E") 1 else -1)
					val alt = altStr.toInt
					(lat, lon, alt)
			}
		}catch{
			case err: Throwable => throw new Exception(
				s"Could not parse $id as stilt site id to extract lat/lon/alt",
				err
			)
		}
	}

	def listFootprints(stationId: String, fromDate: LocalDate, toDate: LocalDate): Iterator[Footprint] =
		if(fromDate.compareTo(toDate) > 0) Iterator.empty else{
			fromDate.getYear.to(toDate.getYear).distinct.toList match{
				case year :: Nil =>
					listFootprints(stationId, year, Some(fromDate), Some(toDate))
				case year1 :: year2 :: Nil =>
					listFootprints(stationId, year1, Some(fromDate), None) ++
					listFootprints(stationId, year2, None, Some(toDate))

				case years =>
					listFootprints(stationId, years.head, Some(fromDate), None) ++
					years.tail.dropRight(1).iterator.flatMap(listFootprints(stationId, _, None, None)) ++
					listFootprints(stationId, years.last, None, Some(toDate))
			}
		}

	private def listFootprints(stationId: String, year: Int, fromDate: Option[LocalDate], toDate: Option[LocalDate]): Iterator[Footprint] = {
		val yearPath = stationsDir.resolve(stationId).resolve(year.toString)

		subdirectories(yearPath).iterator.filter{
			val fromMonth = fromDate.map(_.getMonthValue).getOrElse(0)
			val toMonth = toDate.map(_.getMonthValue).getOrElse(13)

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
			case (path, footDtPattern(yyyy, mm, dd, hh)) =>
				path -> LocalDateTime.of(yyyy.toInt, mm.toInt, dd.toInt, hh.toInt, 0)
		}
		.filter{
			val fromDt = fromDate.map(d => LocalDateTime.of(d, LocalTime.MIN)).getOrElse(LocalDateTime.of(year, 1, 1, 0, 0))
			val toDt = toDate.map(d => LocalDateTime.of(d.plusDays(1), LocalTime.MIN)).getOrElse(LocalDateTime.of(year + 1, 1, 1, 0, 0))
			foot => {
				val dt = foot._2
				dt.compareTo(fromDt) >=0 && dt.compareTo(toDt) < 0
			}
		}
		.scanLeft(new Throttler(LocalDateTime.MIN, None)){(throttler, next) =>
			val nextDt = next._2
			val maxPrevDt = nextDt.minusMinutes(config.slotStepInMinutes)
			if(throttler.lastEmitted.compareTo(maxPrevDt) <= 0)
				new Throttler(nextDt, Some(next))
			else
				new Throttler(throttler.lastEmitted, None)
		}
		.flatMap(_.toEmit)
	}

	def getStiltResultJson(req: StiltResultsRequest): Source[ByteString, NotUsed] = StiltJsonSupport.jsonArraySource(
		() => listFootprints(req.stationId, req.fromDate, req.toDate)
			.sliding(availableProcessors * 5, availableProcessors * 5)
			.flatMap{_.par
				.map{ case (fpFolder, dt) =>

					val row = getCsvRow(fpFolder)

					req.columns
						.map{
							case "isodate" =>
								dt.toEpochSecond(ZoneOffset.UTC).toString
							case col =>
								row.get(col).fold("null")(_.toString)
						}
						.mkString("[", ", ", "]")
				}
				.seq
			}
	)

	private def getCsvRow(footprintFolder: Path): Map[String, Double] = {
		try{
			val fpPath = footprintFolder.resolve(footprintCsvFilename)
			val lines = Files.readAllLines(fpPath)
			val rawRow = RawRow.parse(lines.get(0), lines.get(1))
			ResultRowMaker.makeRow(rawRow)
		}catch{
			case err: Throwable => Map.empty
		}
	}

	private def footPrintDir(stationId: String, dt: LocalDateTime): Path = {
		val yyyy = dt.getYear.toString
		val mm = "%02d".format(dt.getMonthValue)
		val dd = "%02d".format(dt.getDayOfMonth)
		val hh = "%02d".format(dt.getHour)
		val fpDir = Array(yyyy, "x", mm, "x", dd, "x", hh).mkString
		val yyyyDir = stationsDir.resolve(stationId).resolve(yyyy)
		val mmDir = yyyyDir.resolve(mm).resolve(fpDir)

		if(Files.exists(mmDir)) mmDir else{
			val m = dt.getMonthValue.toString
			yyyyDir.resolve(m).resolve(fpDir)
		}
	}

	def getFootprintRaster(stationId: String, dt: LocalDateTime): Raster = {

		val factory = {
			import config.netcdf._
			val footprintDir = footPrintDir(stationId, dt).toString + File.separator
			new ViewServiceFactoryImpl(footprintDir, dateVars.asJava, latitudeVars.asJava, longitudeVars.asJava, elevationVars.asJava)
		}
		val service = factory.getNetCdfViewService("foot")
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

object StiltResultFetcher{

	type Footprint = (Path, LocalDateTime)

	val stationsDirectory = "stations"
	val footprintCsvFilename = "csv"
	// ex: 2007
	val yearDirPattern = """^(\d{4})$""".r
	// ex: 07, 5
	val monthDirPattern = """^(\d\d?)$""".r
	// ex: 2007x02x03x06
	val footDtPattern = """^(\d{4})x(\d\d)x(\d\d)x(\d\d)$""".r
	// ex: 46.55Nx007.98Ex00720
	val siteIdRegex = """^(\d+\.\d+)([NS])x(\d+\.\d+)([EW])x(\d+)$""".r

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

	//def resFileName(year: Int): String = resFileGlob.replace("????", year.toString)

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

	class Throttler(val lastEmitted: LocalDateTime, val toEmit: Option[Footprint])
}
