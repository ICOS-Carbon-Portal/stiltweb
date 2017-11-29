package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path, Paths}

import akka.actor.Actor


class LocalStiltFile (slot: StiltSlot, src: Path, typ: StiltResultFileType.Value) {

	assert(Files.exists(src))

	def link(dir: Path): Unit = {
		val relPath = StiltResultFile.calcFileName(slot, typ)
		val absPath = dir.resolve(relPath)
		// Create leading directories, e.g 'Footprints/XXX/2012'
		Files.createDirectories(absPath.getParent)
		if (! Files.exists(absPath))
			Files.createSymbolicLink(absPath, src)
	}
}


class LocallyAvailableSlot private (val slot: StiltSlot, val slotDir: Path) {

	assert(Files.isDirectory(slotDir))

	final val names = Map(StiltResultFileType.Foot		-> "foot",
						  StiltResultFileType.RDataFoot -> "rdatafoot",
						  StiltResultFileType.RData		-> "rdata")

	final val files = names.map { case (typ, name) =>
		new LocalStiltFile(slot, slotDir.resolve(name), typ) }


	override def toString() = s"LocallyAvailableslot(${slot}, ${slotDir})"

	def link(dir: Path) = {
		files.foreach { f => f.link(dir) }
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
		val slotDir = getSlotDir(slotArchive, result.slot)
		if (! Files.exists(slotDir)) {
			val tmpDir = Files.createDirectories(Paths.get(slotDir + ".tmp"))
			for (f <- result.files) {
				val name = f.typ match {
					case StiltResultFileType.Foot	   => "foot"
					case StiltResultFileType.RDataFoot => "rdatafoot"
					case StiltResultFileType.RData	   => "rdata"
				}
				Util.writeFileAtomically(tmpDir.resolve(name).toFile, f.data)
			}
			Files.move(tmpDir, slotDir)
		} else {
			trace("received a stilt result I already have")
		}
		new LocallyAvailableSlot(result.slot, slotDir)
	}

	def load(slotArchive: Path, slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val slotDir = getSlotDir(slotArchive, slot)
		if (Files.exists(slotDir))
			Some(new LocallyAvailableSlot(slot, slotDir))
		else
			None
	}

	def getSlotDir(slotArchive: Path, slot: StiltSlot): Path = {
		// /some/where/slots/20.01Sx150.01Wx01234/2012/3/2012x12x01x00
		slotArchive.resolve(slot.pos.toString).resolve(
			slot.year.toString).resolve(slot.month.toString).resolve(slot.time.toString)
	}
}


class SlotArchiver(stateDir: Path) extends Actor with Trace{

	val slotsDir = Util.ensureDirectory(stateDir.resolve("slots"))
	traceSetPath(slotsDir.resolve("trace.log"))

	trace(s"Starting up in ${slotsDir}")

	def receive = {
		case SlotCalculated(result) =>
			val local = LocallyAvailableSlot.save(slotsDir, result)(trace)
			sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			LocallyAvailableSlot.load(slotsDir, slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}
}
