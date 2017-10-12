package se.lu.nateko.cp.stiltcluster

import akka.actor.{ Actor, ActorLogging }
import scala.collection.mutable.Queue


class JobMonitor(job: Job) extends Actor with ActorLogging {

	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")
	val jobArchiver = context.actorSelection("/user/jobarchiver")

	slotCalculator ! CalculateSlots(job)

	def receive = {
		case SlotsCalculated(job, slots) =>
			log.info(s"received slots")
			slotProducer ! JobMonitorRegistering
			context become working(Queue(slots: _*))
	}

	def working(slots: Queue[String]): Receive = {
		case SendSlotRequest =>
			log.info("SendSlotRequest")
			if (slots.isEmpty) {
				jobArchiver ! JobFinished(job)
				context.stop(self)
				log.info("JobMonitor all done")
			} else {
				sender() ! SlotRequest(job, slots.dequeue())
			}
		case SlotAvailable(job, slot, data) =>
			log.info(s"Slot available at ${data}")
	}

}
