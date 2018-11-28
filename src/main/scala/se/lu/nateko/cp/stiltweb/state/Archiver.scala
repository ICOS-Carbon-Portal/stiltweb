package se.lu.nateko.cp.stiltweb.state

import java.nio.file.{ Files, Path }
import java.nio.file.attribute.PosixFilePermissions
import java.time.LocalDateTime
import java.time.LocalTime
import scala.io.Source
import se.lu.nateko.cp.stiltweb.csv.LocalDayTime
import se.lu.nateko.cp.stiltweb.csv.RawRow
import se.lu.nateko.cp.stiltweb.csv.ResultRowMaker
import se.lu.nateko.cp.stiltweb.csv.RowCache
import se.lu.nateko.cp.stiltweb.JobDir
import se.lu.nateko.cp.stiltcluster._

class LocallyAvailableSlot (val slot: StiltSlot, val slotDir: Path) {

	assert(Files.isDirectory(slotDir))

	override def toString() = s"LocallyAvailableslot(${slot}, ${slotDir})"
}

class Archiver(val stateDir: Path, slotStepInMinutes: Int) {

	val slotsDir = Util.ensureDirectory(stateDir.resolve("slots"))
	val jobsDir = Util.ensureDirectory(stateDir.resolve("jobs"))
	val stationsDir = Util.ensureDirectory(stateDir.resolve("stations"))

	def save(result: StiltResult): LocallyAvailableSlot = {

		val slotDir = getSlotDir(result.slot)

		if (! Files.exists(slotDir)) {
			Files.createDirectories(slotDir)
			Files.setPosixFilePermissions(slotDir, PosixFilePermissions.fromString("rwxr-xr-x"))
		}

		for (f <- result.files) {
			Util.writeFileAtomically(slotDir.resolve(f.typ.toString), f.data)
		}

		for(csvf <- result.files if csvf.typ == StiltResultFileType.CSV){
			val yearDir = getYearDir(result.slot)
			val year = result.slot.time.year
			val cache = new RowCache(() => Iterator.empty, yearDir, year, slotStepInMinutes)
			val lines = Source.fromBytes(csvf.data, "UTF-8").getLines.toIndexedSeq
			val rawRow = RawRow.parse(lines(0), lines(1))
			val csvRow = ResultRowMaker.makeRow(rawRow)
			cache.writeRow(LocalDayTime(result.slot.time.toJava), csvRow)
		}

		new LocallyAvailableSlot(result.slot, slotDir)
	}

	def load(slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val slotDir = getSlotDir(slot)
		if (
			Files.exists(slotDir) &&
			StiltResultFileType.values.forall { ftype => Files.exists(slotDir.resolve(ftype.toString)) }
		)
			Some(new LocallyAvailableSlot(slot, slotDir))
		else
			None
	}

	def save(job: Job): JobDir = {

		//ensuring that a station symlink exists
		val stationIdLink = getStationDir(job)
		if(!Files.exists(stationIdLink)) Files.createSymbolicLink(stationIdLink, stationsDir.relativize(getPosDir(job.pos)))

		JobDir.saveAsNew(job, getJobDir(job))
	}

	// /some/where/stations/JFJ
	def getJobDir(job: Job): Path = jobsDir.resolve(job.id)

	// /some/where/stations/JFJ
	def getStationDir(job: Job): Path = stationsDir.resolve(job.siteId)

	// /some/where/slots/20.01Sx150.01Wx01234/
	def getPosDir(pos: StiltPosition): Path = slotsDir.resolve(pos.toString)

	// /some/where/slots/20.01Sx150.01Wx01234/2012/
	def getYearDir(slot: StiltSlot): Path =
		getPosDir(slot.pos).resolve(slot.year.toString)

	// /some/where/stations/JFJ/2012/
	def getYearDir(siteId: String, year: Int): Path = stationsDir.resolve(siteId).resolve(year.toString)

	// /some/where/slots/20.01Sx150.01Wx01234/2012/03/2012x12x01x00
	def getSlotDir(slot: StiltSlot): Path =
		getYearDir(slot).resolve(f"${slot.month}%02d").resolve(slot.time.toString)

	// /some/where/stations/JFJ/2012/03/2012x12x01x09
	def getSlotDir(siteId: String, time: StiltTime): Path = stationsDir
		.resolve(siteId).resolve(time.year.toString)
		.resolve(f"${time.month}%02d").resolve(time.toString)

	def calculateSlots(job: Job): Seq[StiltSlot] = {
		val start = LocalDateTime.of(job.start, LocalTime.MIN)
		val stop = LocalDateTime.of(job.stop, LocalTime.MIN).plusDays(1)

		Iterator.iterate(start)(_.plusMinutes(slotStepInMinutes.toLong))
			.takeWhile(_.compareTo(stop) < 0)
			.map{dt =>
				val time = StiltTime(dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour)
				StiltSlot(time, job.pos)
			}
			.toIndexedSeq
	}

}
