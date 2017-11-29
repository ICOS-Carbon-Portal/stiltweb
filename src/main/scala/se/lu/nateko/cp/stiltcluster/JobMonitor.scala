package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}


class JobMonitor(jobDir: JobDir) extends Actor with ActorLogging {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")

	if (jobDir.slots.isEmpty)
		slotCalculator ! CalculateSlotList(jobDir.job)
	else
		 requestRemainingSlots()

	def receive = {
		case SlotListCalculated(slots) =>
			log.info(s"Received a list ${slots.length} slots")
			jobDir.saveSlotList(slots)
			// FIXME - send update to browser about slot having been calculated
			requestRemainingSlots()
	}

	def requestRemainingSlots() = {
		val remaining = jobDir.missingSlots
		log.info(s"${jobDir.slots.get.length} slots in total. ${remaining.length} remaining")
		slotProducer ! RequestManySlots(remaining)
		context become working(remaining)
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
					log.info(s"Received new slot, ${remaining.length} remaining.")
					jobDir.link(local)
				}
			}
			if (remaining.isEmpty)
				done
			else
				context become working(remaining)
	}

	def done() = {
		log.info(s"JobMonitor done, terminating")
		jobDir.markAsDone()
		context stop self
	}
}
