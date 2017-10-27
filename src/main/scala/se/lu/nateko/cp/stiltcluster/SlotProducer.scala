package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Queue, Set}
import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

class SlotProducer extends Actor with ActorLogging {

	val windowMax = 1

	val slotArchive = context.actorSelection("/user/slotarchive")
	val workmasters = Map[ActorRef, Int]()
	val monitors = Set[ActorRef]()
	val window = Queue[(ActorRef, StiltSlot)]()

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
			tick

		case JobMonitorRegistering =>
			if (! monitors.contains(sender)) {
				log.info(s"New JobMonitor ${sender.path}")
				monitors.add(sender)
				context.watch(sender)
			}
			tick

		case RequestManySlots(slots) =>
			log.info(s"Received ${slots.length} slot requests")
			// window.enqueue((sender, slot))
			tick

		case msg: SlotCalculated =>
			log.info("Slot calculated")
			slotArchive ! msg
			tick

		case msg @ SlotAvailable(slot) =>
			log.info("SlotAvailable")
			window.dequeueAll { case (who, slot) =>
				if (slot == slot) {
					who ! msg
					true
				} else {
					false
				}
			}
			tick
	}

	def tick() = {
	//	log.info(s"tick - ${workmasters.size} workmasters. windowsize ${window.size}")
	//	for ((wm, free) <- workmasters) {
	//		log.info(s"Iterating workmaster ${wm}, ${free} cpus free")
	//		for (i <- 1.to(free)) {
	//			if (window.size > 0) {
	//				val (_, job, slot) = window.dequeue()
	//				log.info("Sending CalculateSlot")
	//				wm ! CalculateSlot(slot)
	//				workmasters.update(wm, free-i)
	//			}
	//		}

	//	}
	//	for (_ <- 1.to(windowMax - window.size)) {
	//		if(! monitors.isEmpty) {
	//			log.info("Sending SendSlotRequest")
	//			monitors.head ! SendSlotRequest
	//		} else {
	//			log.info("No monitors")
	//		}
	//	}
	}

}
