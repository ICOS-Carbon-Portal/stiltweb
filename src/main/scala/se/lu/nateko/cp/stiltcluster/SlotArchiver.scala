package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}

import akka.actor.Actor

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

	final val names = Map(StiltResultFileType.Foot      -> "foot",
						  StiltResultFileType.RDataFoot -> "rdatafoot",
						  StiltResultFileType.RData     -> "rdata",
						  StiltResultFileType.CSV       -> "csv")

	final val files = names.map { case (typ, name) =>
		new LocalStiltFile(slot, slotDir.resolve(name), typ) }


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

	def save(slotArchive: Path, result: StiltResult)
			(implicit trace: (String => Unit)): LocallyAvailableSlot = {
		// /disk/data/stiltweb/slots/35.52Nx012.63Ex00010/2013/01/2013x01x25x00
		val slotDir = getSlotDir(slotArchive, result.slot)

		// Case 1 - the slot directory does not exist.
		if (! Files.exists(slotDir)) {
			Files.createDirectories(slotDir.getParent())
			val tmpDir = Files.createTempDirectory(slotDir.getParent(), ".newslot")
			for (f <- result.files) {
				val name = f.typ match {
					case StiltResultFileType.Foot	   => "foot"
					case StiltResultFileType.RDataFoot => "rdatafoot"
					case StiltResultFileType.RData	   => "rdata"
					case StiltResultFileType.CSV	   => "csv"
				}
				Util.writeFileAtomically(tmpDir.resolve(name).toFile, f.data)
			}
			Files.move(tmpDir, slotDir)
		} else {
			// Case 2 - the slot dir exists and is complete.
			if (Files.exists(slotDir.resolve("csv"))) {
				trace(s"received a stilt result I already have")
			}
			// Case 3 - the slot directory exists but the csv file is missing.
			// This is a special case needed when we migrate from saving three
			// files to saving four (the fourth being the csv file)
			else {
				for (f <- result.files) {
					if (f.typ == StiltResultFileType.CSV) {
						Util.writeFileAtomically(slotDir.resolve("csv").toFile, f.data)
					}
				}
			}
		}
		new LocallyAvailableSlot(result.slot, slotDir)
	}

	def load(slotArchive: Path, slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val slotDir = getSlotDir(slotArchive, slot)
		if (Files.exists(slotDir))
			if (StiltResult.requiredFileTypes.forall { typ =>
					val f = StiltResultFile.calcFileName(slot, typ)
						Files.exists(slotDir.resolve(f))})
				Some(new LocallyAvailableSlot(slot, slotDir))
			else
				None
		else
			None
	}

	def getSlotDir(slotArchive: Path, slot: StiltSlot): Path = {
		// /some/where/slots/20.01Sx150.01Wx01234/2012/03/2012x12x01x00
		slotArchive.resolve(slot.pos.toString).resolve(
			slot.year.toString).resolve(f"${slot.month}%02d").resolve(slot.time.toString)
	}
}


class SlotArchiver(stateDir: Path) extends Actor with Trace{

	val slotsDir = Util.ensureDirectory(stateDir.resolve("slots"))
	protected val traceFile = slotsDir.resolve("trace.log")

	trace(s"Starting up in ${slotsDir}")

	def receive = {
		case SlotCalculated(result) =>
			val local = LocallyAvailableSlot.save(slotsDir, result)(trace)
			trace(s"Slot ${local.slotDir} saved.")
			sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			LocallyAvailableSlot.load(slotsDir, slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}
}
