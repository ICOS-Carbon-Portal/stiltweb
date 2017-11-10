package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}


class JobMonitor(jdir: JobDir) extends Actor with ActorLogging {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")

	if (jdir.slots.isEmpty)
		slotCalculator ! CalculateSlotList(jdir.job)
	else
		checkRemainingSlots(None)

	def receive = {
		case SlotListCalculated(slots) =>
			log.info(s"Received a list ${slots.length} slots")
			jdir.saveSlotList(slots)
			log.info(s"Slots saved")
			// FIXME - send update to browser about slot having been calculated
			checkRemainingSlots(Some(slots))
	}

	def checkRemainingSlots(list: Option[Seq[StiltSlot]]) = {
		val remaining = jdir.missingSlots
		log.info(s"${jdir.slots.get.length} slots in total. ${remaining.length} remaining")
		maybeMoreWork(remaining)
	}

	def maybeMoreWork(remaining: Seq[StiltSlot]) = {
		if (remaining.isEmpty) {
			done
		} else {
			slotProducer ! RequestManySlots(remaining)
			context become working(remaining)
		}
	}

	def working(outstanding: Seq[StiltSlot]): Receive = {
		case SlotAvailable(local) =>
			val (removed, remaining) = outstanding.partition(local.equals(_))
			if (removed.isEmpty) {
				log.error(s"Received slot I'm not waiting for ${local}")
			} else {
				jdir.link(local)
				log.info(s"Received now slot, ${local}")
			}
			maybeMoreWork(removed)
	}

	def done() = {
		log.info(s"JobMonitor done, terminating")
		jdir.markAsDone()
		// FIXME
		context stop self
	}
}
