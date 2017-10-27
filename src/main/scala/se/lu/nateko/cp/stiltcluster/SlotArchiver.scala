package se.lu.nateko.cp.stiltcluster

import java.io.File
import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorLogging}


class SlotArchiver(archiveDirectory: File) extends Actor with ActorLogging {

	final val stationDirTemplate = "x%05.02fx%06.02fx%10f"
	final val slotsDir = Paths.get(archiveDirectory.toString).resolve("slots")
	log.info(s"JobArchiver starting up in ${slotsDir}")

	def receive = {
		// FIXME - should be SlotResult
		case SlotCalculated(slot) =>
			log.info(s"Slot calculated ${slot.slot}")
			val local = saveSlot(slot)
			sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			loadSlot(slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}

	private def saveSlot(slot: StiltSlot): LocallyAvailableSlot = {
		val dir = slotDir(slot)
		Files.createDirectory(dir)
		val f = slotFile(slot).toFile
		// FIXME
		f.createNewFile()
		new LocallyAvailableSlot(slot.lat, slot.lon, slot.alt, slot.slot, f)
	}

	private def slotDir(slot: StiltSlot): Path = {
		slotsDir.resolve(s"lat${slot.lat}-lon${slot.lon}-alt${slot.alt}")
	}

	private def slotFile(slot: StiltSlot): Path = {
		slotDir(slot).resolve(slot.slot)
	}

	private def loadSlot(slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val f = slotFile(slot).toFile
		if (f.exists())
			Some(new LocallyAvailableSlot(slot.lat, slot.lon, slot.alt, slot.slot, f))
		else
			None
	}


	// private def stationDir(slot: StiltSlot): File = {
	//	new File(archiveDirectory, stationDirTemplate.format(
	//				 slot.lat, slot.lon, slot.alt))
	// }

	// private def slotDir(jslot: StiltSlot): File = {
	//	new File(new File(stationDir(jslot), jslot.year), jslot.month)
	// }

}
