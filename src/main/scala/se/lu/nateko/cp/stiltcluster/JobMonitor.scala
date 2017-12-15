package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor


class JobMonitor(jobDir: JobDir) extends Actor with Trace {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")

	protected val traceFile = jobDir.dir.resolve("trace.log")

	trace(s"Starting up in ${jobDir.dir}")

	if (jobDir.slots.isEmpty) {
		trace("No slot list, requesting one.")
		slotCalculator ! CalculateSlotList(jobDir.job)
	} else {
		trace("Already have slot list")
		requestRemainingSlots()
	}

	def receive = {
		case SlotListCalculated(slots) =>
			trace(s"Received a list of ${slots.length} slots")
			jobDir.saveSlotList(slots)
			// FIXME - send update to browser about slot having been calculated
			requestRemainingSlots()
	}

	def requestRemainingSlots() = {
		val remaining = jobDir.missingSlots
		trace(s"${jobDir.slots.get.length} slots in total. ${remaining.length} remaining, sending request.")
		slotProducer ! RequestManySlots(remaining)
		context become working(remaining)
	}

	def working(outstanding: Seq[StiltSlot]): Receive = {
		case SlotAvailable(local) =>
			val (removed, remaining) = outstanding.partition(local.equals(_))
			if (removed.isEmpty) {
				trace(s"Received slot I'm not waiting for ${local}")
			} else {
				if (jobDir.slotPresent(local)) {
					trace("Received a slot that is already present")
				} else {
					trace(s"Received new slot, ${remaining.length} remaining.")
					jobDir.link(local)
				}
			}
			if (remaining.isEmpty)
				done
			else
				context become working(remaining)
	}

	def done() = {
		trace(s"Done, terminating")
		jobDir.markAsDone()
		context stop self
	}
}
