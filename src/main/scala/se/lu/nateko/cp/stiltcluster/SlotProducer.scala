package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Queue, Set}
import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

class SlotProducer extends Actor with ActorLogging {

	val windowMax = 1

	val slotArchive = context.actorSelection("/user/slotarchive")
	val workmasters = Map[ActorRef, Int]()
	val monitors = Set[ActorRef]()
	val window = Queue[(ActorRef,Job,String)]()

	def receive = {
		case WorkMasterStatus(freeCores) =>
			if (! workmasters.contains(sender)) {
				log.info(s"New WorkMaster ${sender.path}, ${freeCores} free cores")
				context.watch(sender)
			}
			workmasters.update(sender, freeCores)
			tick

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				log.info(s"WorkMaster ${sender.path} terminated")
				workmasters.remove(dead)
			} else {
				log.info("JobMonitor terminated")
				monitors.remove(dead)
			}

		case JobMonitorRegistering =>
			if (! monitors.contains(sender)) {
				log.info(s"New JobMonitor ${sender.path}")
				context.watch(sender)
			}

		case SlotRequest(job, slot) =>
			log.info("Received slot request")
			window.enqueue((sender, job, slot))

		case msg: SlotCalculated =>
			log.info("Slot calculated")
			slotArchive ! msg

		case msg @ SlotAvailable(job, slot, _) =>
			log.info("SlotAvailable")
			window.dequeueAll { case (who, job, slot) =>
				if (job == job && slot == slot) {
					who ! msg
					true
				} else {
					false
				}
			}
			tick
	}

	def tick = {
		for ((wm, free) <- workmasters) {
			for (i <- 1.to(free)) {
				if (window.size > 0) {
					val (_, job, slot) = window.dequeue()
					wm ! CalculateSlot(job, slot)
					workmasters.update(wm, free-i)
				}
			}

		}
		for (_ <- 1.to(windowMax - window.size)) {
			log.info("Sending SendSlotRequest")
			monitors.head ! SendSlotRequest
		}
	}
}
