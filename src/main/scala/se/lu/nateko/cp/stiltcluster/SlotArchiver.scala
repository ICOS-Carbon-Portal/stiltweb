package se.lu.nateko.cp.stiltcluster

import java.io.File
import akka.actor.{Actor, ActorLogging}


class SlotArchiver(stateDir: File) extends Actor with ActorLogging {

	final val stationDirTemplate = "x%05.02fx%06.02fx%10f"
	final val slotsDir = new File(stateDir, "slots")
	if (! slotsDir.isDirectory()) {
		slotsDir.mkdir()
	}

	log.info(s"starting up in ${slotsDir}")

	def receive = {
		// // FIXME - slot should be result
		// case SlotCalculated(slot) =>
		//	// FIXME
		//	log.info(s"Slot calculated ${slot.slot}")
		//	val local = saveSlot(slot)
		//	sender() ! SlotAvailable(local)

		case RequestSingleSlot(slot) =>
			log.info("Receiving single slot request")
			loadSlot(slot) match {
				case None => sender() ! SlotUnAvailable(slot)
				case Some(local) => sender ! SlotAvailable(local)
			}
	}

	// FIXME
	// private def saveSlot(slot: StiltSlot): LocallyAvailableSlot = {
	//	slotDir(slot).mkdir()
	//	val f = slotFile(slot)
	//	// FIXME
	//	f.createNewFile()
	//	new LocallyAvailableSlot(slot, f)
	// }

	private def loadSlot(slot: StiltSlot): Option[LocallyAvailableSlot] = {
		val f = slotFile(slot)
		if (f.exists())
			Some(new LocallyAvailableSlot(slot, f))
		else
			None
	}

	private def slotDir(slot: StiltSlot): File = {
		new File(slotsDir, s"lat${slot.lat}xlon${slot.lon}xalt${slot.alt}")
	}

	private def slotFile(slot: StiltSlot): File = {
		// FIXME
		new File(slotDir(slot), "slot")
	}

}
