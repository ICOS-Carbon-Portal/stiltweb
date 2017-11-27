package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}


class JobMonitor(jobDir: JobDir) extends Actor with ActorLogging {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")

	if (jobDir.slots.isEmpty)
		slotCalculator ! CalculateSlotList(jobDir.job)
	else
		checkRemainingSlots(None)

	def receive = {
		case SlotListCalculated(slots) =>
			log.info(s"Received a list ${slots.length} slots")
			jobDir.saveSlotList(slots)
			log.info(s"Slots saved")
			// FIXME - send update to browser about slot having been calculated
			checkRemainingSlots(Some(slots))
	}

	def checkRemainingSlots(list: Option[Seq[StiltSlot]]) = {
		val remaining = jobDir.missingSlots
		log.info(s"${jobDir.slots.get.length} slots in total. ${remaining.length} remaining")
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
				if (jobDir.slotPresent(local)) {
					log.info("received a slot that is already present")
				} else {
					log.info(s"Received new slot, ${local}")
					jobDir.link(local)
				}
			}
			maybeMoreWork(removed)
	}

	def done() = {
		log.info(s"JobMonitor done, terminating")
		jobDir.markAsDone()
		// FIXME
		context stop self
	}
}
