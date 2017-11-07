package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorLogging}


class SlotArchiver(stateDir: Path) extends Actor with ActorLogging {

	final val stationDirTemplate = "x%05.02fx%06.02fx%10f"
	final val slotsDir = stateDir.resolve("slots")
	if (! Files.isDirectory(stateDir))
		Files.createDirectory(stateDir)

	log.info(s"starting up in ${slotsDir}")

	def receive = {
		case SlotCalculated(result) =>
			log.info(s"Slot calculated ${result.slot}")
			val local = saveResult(result)
			sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			log.info("Receiving single slot request")
			loadSlot(slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}

	private def saveResult(result: StiltResult): LocallyAvailableSlot = {
		val sdir = getSlotDir(result.slot)
		if (Files.isDirectory(sdir)) {
			log.warning("slot ${slot} already existed on disk")
		} else {
			val tmp = Paths.get(sdir + ".tmp")
			if (Files.isDirectory(tmp))
				log.warning(s"temporary slot directory $tmp already exists, removing")
			else
				Files.createDirectory(tmp)
			for (f <- result.files) {
				val name = f.typ match {
					case StiltResultFileType.Foot => "foot"
					case StiltResultFileType.RDataFoot => "rdatafoot"
					case StiltResultFileType.RData => "rdata"
				}
				val dst = tmp.resolve(name)
				Util.writeFileAtomically(dst.toFile, f.data)
			}
			Files.move(tmp, sdir)
		}
		LocallyAvailableSlot(result.slot, sdir)
	}

	private def loadSlot(slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val d = getSlotDir(slot)
		if (Files.exists(d))
			Some(LocallyAvailableSlot(slot, d))
		else
			None
	}

	private def getSlotDir(slot: StiltSlot): Path = {
		// /some/where/slots/20.01Sx150.01Wx01234/2012/3/2012x12x01x00
		slotsDir.resolve(slot.pos.toString).resolve(
			slot.year.toString).resolve(slot.month.toString).resolve(slot.time.toString)
	}
}
