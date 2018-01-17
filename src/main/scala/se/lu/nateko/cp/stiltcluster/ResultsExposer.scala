package se.lu.nateko.cp.stiltcluster

import java.nio.file.Files
import java.nio.file.Path

import scala.io.Source

import se.lu.nateko.cp.stiltweb.StiltResultFetcher

class ResultsExposer(mainDir: Path) {

	import se.lu.nateko.cp.stiltweb.StiltResultFetcher.{ footPrintsDirectory, listFiles, resDirectory }

	private val footsDir = mainDir.resolve(footPrintsDirectory)
	private val resDir = mainDir.resolve(resDirectory)

	def expose(jobDir: JobDir): Unit = {

		val stationFootsDir = footsDir.resolve(jobDir.job.siteId)
		Files.createDirectories(stationFootsDir)

		listFiles(jobDir.dir.resolve(s"slots/$footPrintsDirectory/XXX"), "foot*.nc")
			.foreach{linkToFoot =>
				val result = stationFootsDir.resolve(linkToFoot.getFileName)
				Files.deleteIfExists(result)
				Files.createSymbolicLink(result, linkToFoot.toRealPath())
			}

		val stationResDir = resDir.resolve(jobDir.job.siteId)
		Files.createDirectories(stationResDir)

		listFiles(jobDir.dir.resolve(s"merge/output/$resDirectory/XXX"), "stiltresult*.csv")
			.flatMap(StiltResultsCsv.apply)
			.foreach{result =>
				val target = stationResDir.resolve(result.stiltFileName)
				StiltResultsCsv(target).foreach(result.mergeWith)
				result.writeToFile(target)
			}
	}
}

class StiltResultsCsv private(val year: Int, lines: Iterator[String]){

	val stiltFileName = StiltResultFetcher.resFileGlob.replace("????", year.toString)

	private[this] val header = lines.next()
	private var entries: Map[String, String] = lines.map(line => (line.split(" ")(0), line)).toMap

	def fileLines: Seq[String] = header +: entries.keys.toSeq.sorted.map(entries.apply)

	def mergeWith(other: StiltResultsCsv): Unit = {
		entries = other.entries ++ entries //own entries take precedence
	}

	def writeToFile(file: Path): Unit = {
		import scala.collection.JavaConverters._
		Files.write(file, fileLines.asJava)
	}
}

object StiltResultsCsv{

	private val stiltPattern = StiltResultFetcher.resFilePattern
	private val andrePattern = "stiltresult(\\d{4}).+\\.csv".r

	def apply(file: Path): Option[StiltResultsCsv] = {

		def fromYear(year: String) = {
			val lines = Source.fromFile(file.toFile).getLines
			if(lines.hasNext) Some(new StiltResultsCsv(year.toInt, lines))
			else None
		}

		if(!Files.exists(file)) None
		else file.getFileName.toString match {
			case stiltPattern(year) => fromYear(year)
			case andrePattern(year) => fromYear(year)
			case _ => None
		}
	}
}
