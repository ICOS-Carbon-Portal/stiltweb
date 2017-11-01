package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}


class JobMonitor(jdir: JobDir) extends Actor with ActorLogging {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")

	if (jdir.slots.isEmpty) {
		slotCalculator ! CalculateSlotList(jdir.job)
	} else {
		log.info(s"Slots already present")
		checkPresentSlots
	}

	def receive = {
		case SlotListCalculated(slots) =>
			log.info(s"Received slots")
			jdir.saveSlotList(slots)
			// FIXME - send update to browser about slot having been calculated
			checkPresentSlots
	}

	def checkPresentSlots() = {
		val remaining = jdir.missingSlots
		log.info(s"I have ${remaining.length} slots left.")
		slotProducer ! RequestManySlots(remaining)
		context become working(remaining)
	}

	def working(outstanding: Seq[StiltSlot]): Receive = {
		case SlotAvailable(slot) =>
			val (removed, remaining) = outstanding.partition(slot.equals(_))
			if (removed.isEmpty) {
				log.error(s"Received slot I'm not waiting for ${slot}")
			} else {
				val link = jdir.linkSlot(slot)
				log.info(s"Received now slot, ${slot}. Linked to ${link}")
			}
			if (remaining.isEmpty) {
				log.info(s"JobMonitor done, terminating")
				jdir.markAsDone()
				// FIXME
				context stop self
			} else {
				context become working(remaining)
			}
	}
}
