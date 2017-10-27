package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging}
import se.lu.nateko.cp.stiltrun.RunStilt

class SlotCalculator extends Actor with ActorLogging {

	val slotArchive = context.actorSelection("/user/slotarchive")

	def receive = {
		case CalculateSlotList(job) =>
			log.info(s"Asked to calculate slots for job $job")
			val origin = sender ()
			Future {
				val slots = RunStilt.cmd_calcslots(job.start, job.stop).map { job.getSlot(_) }
				log.info(s"Slots calculated (all ${slots.length} of them)")
				origin ! SlotListCalculated(slots)
			}
			log.info("Started slot calculation in separate future")
	}
}
