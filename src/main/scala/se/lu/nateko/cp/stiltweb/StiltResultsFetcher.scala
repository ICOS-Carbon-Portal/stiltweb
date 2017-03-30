package se.lu.nateko.cp.stiltweb

import java.nio.file.Paths
import java.io.File
import java.nio.file.Files
import scala.collection.JavaConversions._
import java.nio.file.Path
import akka.stream.scaladsl.Source
import scala.io.{Source => IoSource}
import akka.util.ByteString
import akka.NotUsed
import se.lu.nateko.cp.data.formats.netcdf.viewing.Raster
import se.lu.nateko.cp.data.formats.netcdf.viewing.impl.ViewServiceFactoryImpl


class StiltResultsFetcher(config: StiltWebConfig, jobId: Option[String] = None) {
	import StiltResultFetcher._

	val mainDirectory = jobId match{
		case None => config.mainDirectory
		case Some(job) => config.jobsOutputDirectory + "/" + job
	}

	private def listSubdirectories(parent: String, child: String): Array[File] = {
		new File(parent, child).listFiles() match {
			// listFiles() will return null (!) if the directory doesn't exist.
			case null  => Array.empty[File]
			case files => files.filter(_.isDirectory)
		}
	}

	def resFileName(year: Int): String = resFileGlob.replace("????", year.toString)

	def getStationInfos: Seq[StiltStationInfo] = {
		val stiltToIcos: Map[String, String] = config.stations.collect{
			case Seq(stilt, icos, _) if !icos.isEmpty => (stilt, icos)
		}.toMap

		val stiltToWdcgg: Map[String, String] = config.stations.collect{
			case Seq(stilt, _, wdcgg) if !wdcgg.isEmpty => (stilt, wdcgg)
		}.toMap

		val stiltToYears: Map[String, Seq[Int]] = getStationYears

		val stationFpDirectories = listSubdirectories(mainDirectory, footPrintsDirectory)

		stationFpDirectories.map{directory =>
			val stiltId = directory.getName
			val (lat, lon, alt) = latLonAlt(directory)
			val years = stiltToYears.get(stiltId).getOrElse(Nil)
			StiltStationInfo(stiltId, lat, lon, alt, years, stiltToIcos.get(stiltId), stiltToWdcgg.get(stiltId))
		}
	}

	private def getStationYears: Map[String, Seq[Int]] = {

		def stationYears(dir: File): Seq[Int] = listFileNames(dir.toPath, resFileGlob).collect{
			case resFilePattern(dddd) => dddd.toInt
		}

		val stationDirectories = listSubdirectories(mainDirectory, resDirectory)

		stationDirectories.map(stFold => (stFold.getName, stationYears(stFold))).toMap
	}

	private def latLonAlt(footPrintsDirectory: File): (Double, Double, Int) = {
		val fpFileNames = listFileNames(footPrintsDirectory.toPath, "foot*.nc", Some(5))

		val latLonAlts = fpFileNames.collect{
			case fp @ fpnameRegex(latStr, latSignStr, lonStr, lonSignStr, altStr) =>
				try{
					val lat = latStr.toDouble * (if(latSignStr == "N") 1 else -1)
					val lon = lonStr.toDouble * (if(lonSignStr == "E") 1 else -1)
					val alt = altStr.toInt
					(lat, lon, alt)
				}catch{
					case err: Throwable => throw new Exception(
						s"Could not parse footprint filename $fp to extract lat/lon/alt",
						err
					)
				}
		}

		latLonAlts.headOption.getOrElse(throw new Exception(
			s"Could not find a parseable footprint file in directory ${footPrintsDirectory.getAbsolutePath}")
		)
	}

	def getFootprintFiles(stationId: String, year: Int): Seq[String] = {
		val stationPath = Paths.get(mainDirectory, footPrintsDirectory, stationId)
		listFileNames(stationPath, "foot" + year + "*.nc")
	}

	def getStiltResultJson(stationId: String, year: Int, columns: Seq[String]): Source[ByteString, NotUsed] = {
		val resultsPath = Paths.get(mainDirectory, resDirectory, stationId, resFileName(year))
		val src = IoSource.fromFile(resultsPath.toFile)
		NumericScv.getJsonSource(src, columns)
	}

	def getFootprintRaster(stationId: String, filename: String): Raster = {
		val factory = {
			import config.netcdf._
			import scala.collection.JavaConversions._
			val footprintsDirectories = Paths.get(mainDirectory, footPrintsDirectory, stationId).toString + File.separator
			new ViewServiceFactoryImpl(footprintsDirectories, dateVars, latitudeVars, longitudeVars, elevationVars)
		}
		val service = factory.getNetCdfViewService(filename)
		val date = service.getAvailableDates()(0)
		service.getRaster(date, "foot", null)
	}
}

object StiltResultFetcher{

	val resFileGlob = "stiltresults????.csv"
	val resFilePattern = resFileGlob.replace("????", "(\\d{4})").r
	val resDirectory = "Results"
	val footPrintsDirectory = "Footprints"
	val fpnameRegex = """^foot\d{4}x\d\dx\d\dx\d\dx(\d+\.\d+)([NS])x(\d+\.\d+)([EW])x(\d+).+$""".r

	def listFileNames(dir: Path, fileGlob: String, limit: Option[Int] = None): Seq[String] = {
		val dirStream = Files.newDirectoryStream(dir, fileGlob)
		try{
			val fnameIter = dirStream.iterator().map(_.getFileName.toString)
			(limit match{
				case None => fnameIter
				case Some(lim) => fnameIter.take(lim)
			}).toIndexedSeq
		} finally {
			dirStream.close()
		}
	}
}
