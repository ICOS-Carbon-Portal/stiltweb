package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Queue}

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

class SlotProducer extends Actor with ActorLogging {

	val slotArchiver = context.actorSelection("/user/slotarchiver")

	val windowMax = 1
	val workmasters = Map[ActorRef, Int]()
	val requests = Map[StiltSlot, Seq[ActorRef]]()
	val slots = Queue[StiltSlot]()
//	val tick = system.scheduler.schedule

	def receive = {
		case WorkMasterStatus(freeCores) =>
			if (! workmasters.contains(sender)) {
				log.info(s"New WorkMaster ${sender.path}, ${freeCores} free cores")
				context.watch(sender)
			}
			workmasters.update(sender, freeCores)
			maybeSend

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				log.info(s"WorkMaster ${sender.path} terminated")
				workmasters.remove(dead)
			}

		case RequestManySlots(slots) =>
			log.info(s"Received ${slots.length} slot requests")
			for (slot <- slots) {
				requests.update(slot, requests.getOrElse(slot, List()) :+ sender())
				log.info("Sending single slot request to slotarchiver")
				slotArchiver ! RequestSingleSlot(slot)
			}

		case msg: SlotCalculated => {
			log.info("Got SlotCalculated, sending to slot archive")
			slotArchiver ! msg
		}

		case msg @ SlotAvailable(local) =>
			log.info("SlotAvailable(slot)")
			requests.remove(local.slot) match {
				case None => log.warning(s"SlotAvailable(${local}) with no requests")
				case Some(actors) => actors.foreach { _ ! msg }
			}

		case SlotUnAvailable(slot) =>
			log.info("SlotUnavailable")
			slots.enqueue(slot)
			maybeSend

	}

	private def maybeSend() = {
		for ((wm, freeCores) <- workmasters) {
			for (_ <- 1 to freeCores) {
				// FIXME: Set timeout
				if (! slots.isEmpty) {
					log.info("Sending slot to workmaster")
					sender ! CalculateSlot(slots.dequeue)
				}
			}
		}
	}
}
