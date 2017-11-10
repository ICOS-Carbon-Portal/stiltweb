package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Set}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}


class SlotProducer extends Actor with ActorLogging {

	case object Tick
	final val tickInterval = 5 seconds
	final val calcTimeout = 120 seconds

	val slotArchiver = context.actorSelection("/user/slotarchiver")

	val windowMax = 1
	val workmasters = Map[ActorRef, Int]()
	val requests = Map[StiltSlot, Seq[ActorRef]]()
	// FIXME: the mutable Queue isn't able to add to front !?
	var waiting = scala.collection.immutable.Queue[StiltSlot]()
	val sent = Set[(Deadline, StiltSlot)]()

	scheduleTick

	def receive = {
		case WorkMasterStatus(freeCores) =>
			if (! workmasters.contains(sender)) {
				log.info(s"New WorkMaster ${sender}, ${freeCores} free cores")
				context.watch(sender)
			}
			workmasters.update(sender, freeCores)

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
			waiting = waiting.enqueue(slot)

		case Tick =>
			log.info("Tick!")
			log.info(s"${requests.size} requests, ${waiting.length} in queue, ${sent.size} sent")
			checkTimeouts
			sendSomeSlots
			scheduleTick
	}


	private def scheduleTick() = {
		context.system.scheduler.scheduleOnce(tickInterval, self, Tick)
	}

	private def checkTimeouts() = {
		val overdue = sent.filter { case (deadline, slot) => deadline.isOverdue }
		overdue.foreach { case elem @ (deadline, slot) =>
			log.warning(s"Slot ${slot} is overdue, requeuing")
			waiting = slot +: waiting
			sent.remove(elem)
		}
	}

	private def sendSomeSlots() =
		try {
			for ((wm, freeCores) <- workmasters) {
				log.info(s"Considering workmaster ${wm} with ${freeCores} free cores")
				for (_ <- 1 to freeCores) {
					val (slot: StiltSlot, remaining) = waiting.dequeue
					waiting = remaining
					val deadline = calcTimeout.fromNow
					sent.add((deadline, slot))
					wm ! CalculateSlot(slot)
					log.info(s"Sent slot to workmaster (timeout in ${calcTimeout} seconds)")
				}
			}
		} catch {
				case _: java.util.NoSuchElementException =>
					log.info("No slots waiting")
		}
}
