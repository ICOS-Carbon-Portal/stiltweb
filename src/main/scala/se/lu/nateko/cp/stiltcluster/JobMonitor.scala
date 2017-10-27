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
			checkPresentSlots
	}

	def checkPresentSlots() = {
		val remaining = jdir.findMissingSlots
		slotProducer ! RequestManySlots(remaining)
		context become working(remaining)
	}

	def working(outstanding: Seq[StiltSlot]): Receive = {
		case SlotAvailable(slot) =>
			val (remaining, removed) = outstanding.partition(slot.equals(_))
			if (removed.isEmpty) {
				log.error(s"Received slot I'm not waiting for ${slot.slot}")
			} else {
				log.info(s"Received now slot, ${slot.slot}")
				jdir.linkSlot(slot)
			}
			context become working(remaining)
	}

}
