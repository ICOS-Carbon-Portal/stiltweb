package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging}
import se.lu.nateko.cp.stiltweb.RunStilt

class SlotCalculator extends Actor with ActorLogging {

	val slotArchive = context.actorSelection("/user/slotarchive")

	def receive = {
		case CalculateSlots(job) =>
			log.info(s"Asked to calculate slots for job $job")
			val origin = sender ()
			Future {
				val slots = RunStilt.cmd_calcslots(job)
				log.info(s"Slots calculated (all ${slots.length} of them)")
				origin ! SlotsCalculated(job, slots)
			}
			log.info("Started slot calculation in separate future")
	}
}
