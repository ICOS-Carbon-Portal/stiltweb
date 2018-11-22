package se.lu.nateko.cp.stiltcluster

import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import scala.io.Source

import se.lu.nateko.cp.stiltweb.csv.RawRow
import se.lu.nateko.cp.stiltweb.csv.LocalDayTime
import se.lu.nateko.cp.stiltweb.csv.ResultRowMaker
import se.lu.nateko.cp.stiltweb.csv.RowCache

class LocallyAvailableSlot private (val slot: StiltSlot, val slotDir: Path) {

	assert(Files.isDirectory(slotDir))

	override def toString() = s"LocallyAvailableslot(${slot}, ${slotDir})"
}


object LocallyAvailableSlot {

	def save(slotArchive: Path, result: StiltResult, slotStepInMinutes: Int): LocallyAvailableSlot = {

		val slotDir = getSlotDir(slotArchive, result.slot)

		if (! Files.exists(slotDir)) {
			Files.createDirectories(slotDir)
			Files.setPosixFilePermissions(slotDir, PosixFilePermissions.fromString("rwxr-xr-x"))
		}

		for (f <- result.files) {
			Util.writeFileAtomically(slotDir.resolve(f.typ.toString), f.data)
		}

		for(csvf <- result.files if csvf.typ == StiltResultFileType.CSV){
			val yearDir = getYearDir(slotArchive, result.slot)
			val year = result.slot.time.year
			val cache = new RowCache(() => Iterator.empty, yearDir, year, slotStepInMinutes)
			val lines = Source.fromBytes(csvf.data, "UTF-8").getLines.toIndexedSeq
			val rawRow = RawRow.parse(lines(0), lines(1))
			val csvRow = ResultRowMaker.makeRow(rawRow)
			cache.writeRow(LocalDayTime(result.slot.time.toJava), csvRow)
		}

		new LocallyAvailableSlot(result.slot, slotDir)
	}

	def load(slotArchive: Path, slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val slotDir = getSlotDir(slotArchive, slot)
		if (
			Files.exists(slotDir) &&
			StiltResultFileType.values.forall { ftype => Files.exists(slotDir.resolve(ftype.toString)) }
		)
			Some(new LocallyAvailableSlot(slot, slotDir))
		else
			None
	}

	// /some/where/slots/20.01Sx150.01Wx01234/2012/
	def getYearDir(slotArchive: Path, slot: StiltSlot): Path =
		slotArchive.resolve(slot.pos.toString).resolve(slot.year.toString)

	// /some/where/slots/20.01Sx150.01Wx01234/2012/03/2012x12x01x00
	def getSlotDir(slotArchive: Path, slot: StiltSlot): Path =
		getYearDir(slotArchive, slot).resolve(f"${slot.month}%02d").resolve(slot.time.toString)
}


class SlotArchiver(stateDir: Path, slotStepInMinutes: Int) {

	val slotsDir = Util.ensureDirectory(stateDir.resolve("slots"))

	def save(result: StiltResult) = LocallyAvailableSlot.save(slotsDir, result, slotStepInMinutes)

	def load(slot: StiltSlot) = LocallyAvailableSlot.load(slotsDir, slot)
}
