package se.lu.nateko.cp.stiltcluster

import java.nio.file.Path

import scala.collection.mutable.{Map, Set}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Terminated}


class SlotProducer (tracePath: Path) extends Actor with Trace {

	traceSetPath(tracePath)

	case object Tick
	final val tickInterval = 5 seconds
	final val calcTimeout = 120 seconds

	val slotArchiver = context.actorSelection("/user/slotarchiver")

	val workmasters = Map[ActorRef, Int]()
	val requests = Map[StiltSlot, Seq[ActorRef]]()
	var waiting = Array.empty[StiltSlot]
	val sent = Set[(Deadline, StiltSlot)]()

	var previousStatus = getStatus

	def getStatus =
		(workmasters.size, workmasters.values.sum, requests.size, waiting.size, sent.size)

	scheduleTick

	def receive = {
		case WorkMasterStatus(freeCores) =>
			if (! workmasters.contains(sender)) {
				trace(s"New WorkMaster ${sender}, ${freeCores} free cores")
				context.watch(sender)
			}
			trace(s"Updating WorkMaster status for ${sender} to ${freeCores} free cores")
			workmasters.update(sender, freeCores)

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				trace(s"WorkMaster ${sender.path} terminated")
				workmasters.remove(dead)
			}

		case RequestManySlots(slots) =>
			trace(s"Received a request for ${slots.length} slots.")
			for (slot <- slots) {
				requests.update(slot, requests.getOrElse(slot, List()) :+ sender())
				slotArchiver ! RequestSingleSlot(slot)
			}
			trace(s"Passed ${slots.length} requests on to the slot archiver")

		case msg @ SlotCalculated(result) => {
			trace("Got SlotCalculated, sending on to slot archive")
			slotArchiver ! msg
			waiting = waiting.filter { _ != result }
		}

		case msg @ SlotAvailable(local) =>
			requests.remove(local.slot) match {
				case None => trace(s"${local.slotDir} with no requests!")
				case Some(actors) => {
					actors.foreach { _ ! msg }
					trace(s"${local.slotDir} passed on to ${actors.size} actors")
				}
			}

		case SlotUnAvailable(slot) =>
			waiting = waiting :+ slot

		case Tick =>
			val status = getStatus
			if (status != previousStatus) {
				val (nWm, nCpus, nR, nW, nS) = status
				// Only trace when something has changed, to avoid spamming the log
				trace(s"Tick - $nWm workmasters (${nCpus} cpus), $nR requests, $nW waiting, $nS sent")
				previousStatus = status
			}
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
			sent.remove(elem)
			if (! requests.contains(slot)) {
				trace(s"Overdue slot ${slot} no longer requested")
			} else {
				trace(s"Overdue slot ${slot} still requested, bouncing off slot archiver")
				// Before requeueing the slot, check (again) that it isn't archived.
				slotArchiver ! RequestSingleSlot(slot)
			}
		}
	}

	private def sendSomeSlots():Unit = {
		for ((wm, freeCores) <- workmasters) {
			for (i <- freeCores-1 to 0 by -1) {
				if (waiting.isEmpty)
					return
				val slot = waiting.head
				waiting = waiting.drop(1)
				sent.add((calcTimeout.fromNow, slot))
				wm ! CalculateSlot(slot)
				trace(s"Sent slot to ${wm} (timeout in ${calcTimeout} seconds)")
				// Keep track of the number of available slot ourselves.
				// This will get overwritten once the workmaster reports in
				// again.
				workmasters.update(wm, i)
			}
		}
	}
}
