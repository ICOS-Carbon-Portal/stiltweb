package se.lu.nateko.cp.stiltcluster

import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import scala.io.Source

import akka.actor.Actor
import se.lu.nateko.cp.stiltweb.csv.RawRow
import se.lu.nateko.cp.stiltweb.csv.LocalDayTime
import se.lu.nateko.cp.stiltweb.csv.ResultRowMaker
import se.lu.nateko.cp.stiltweb.csv.RowCache
import akka.actor.Props

case class CsvMissing() extends Exception

class LocalStiltFile (slot: StiltSlot, src: Path, typ: StiltResultFileType.Value) {

	if (! Files.exists(src)) {
		if (typ == StiltResultFileType.CSV)
			throw new CsvMissing()
		else
			throw new AssertionError(s"${src} is missing from slot directory")
	}

	def link(dir: Path): Unit = {
		val relPath = StiltResultFile.calcFileName(slot, typ)
		val absPath = dir.resolve(relPath)
		// Create leading directories, e.g 'Footprints/XXX'
		Files.createDirectories(absPath.getParent)
		if (! Files.exists(absPath))
			Files.createSymbolicLink(absPath, src)
	}
}


class LocallyAvailableSlot private (val slot: StiltSlot, val slotDir: Path) {

	assert(Files.isDirectory(slotDir))

	final val files = StiltResultFileType.values.map { ftyp =>
		new LocalStiltFile(slot, slotDir.resolve(ftyp.toString), ftyp) }

	override def toString() = s"LocallyAvailableslot(${slot}, ${slotDir})"

	def link(dir: Path) = {
		files.foreach { f =>
			try {
				f.link(dir)
			} catch {
				// TODO. Wte catch this error for now. The reason is that we
				// have a lot of slots for which we have three of the four
				// files, only the csv is missing. Once all slots have four
				// files we can remove this clause again.
				case _: java.nio.file.FileAlreadyExistsException => ()
			}
		}
	}

	def equals(o: StiltSlot) = {
		this.slot == o
	}
}


object LocallyAvailableSlot {

	def isLinked(jobDir: Path, slot: StiltSlot): Boolean = {
		StiltResult.requiredFileTypes.forall { typ =>
			val f = StiltResultFile.calcFileName(slot, typ)
			Files.exists(jobDir.resolve(f))
		}
	}

	def save(slotArchive: Path, result: StiltResult, slotStepInMinutes: Int): LocallyAvailableSlot = {

		val slotDir = getSlotDir(slotArchive, result.slot)

		if (! Files.exists(slotDir)) {
			Files.createDirectories(slotDir)
			Files.setPosixFilePermissions(slotDir, PosixFilePermissions.fromString("rwxr-xr-x"))
		}

		for (f <- result.files) {
			Util.writeFileAtomically(slotDir.resolve(f.typ.toString).toFile, f.data)
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


class SlotArchiver(stateDir: Path, slotStepInMinutes: Integer) extends Actor with Trace{

	val slotsDir = Util.ensureDirectory(stateDir.resolve("slots"))
	protected val traceFile = slotsDir.resolve("trace.log")

	trace(s"Starting up in ${slotsDir}")

	def receive = {
		case SlotCalculated(result) =>
			val local = LocallyAvailableSlot.save(slotsDir, result, slotStepInMinutes)
			trace(s"Slot ${local.slotDir} saved.")
			sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			LocallyAvailableSlot.load(slotsDir, slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}
}

object SlotArchiver{
	def props(stateDir: Path, slotStepInMinutes: Integer) = Props.create(classOf[SlotArchiver], slotStepInMinutes)
}
