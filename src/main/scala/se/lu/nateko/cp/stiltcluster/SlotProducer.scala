package se.lu.nateko.cp.stiltcluster

import java.nio.file.Path

import scala.collection.mutable.{Map, Set}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Terminated}


class SlotProducer (protected val traceFile: Path) extends Actor with Trace {

	case object Tick
	final val tickInterval = 5 seconds
	//TODO Replace timeout with DeathWatch on work masters
	final val calcTimeout = 400 seconds

	val slotArchiver = context.actorSelection("/user/slotarchiver")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	val workmasters = Map[ActorRef, Int]()
	val requests = Map[StiltSlot, Seq[ActorRef]]()
	var waiting = Array.empty[StiltSlot]
	val sent = Set[(Deadline, StiltSlot)]()

	var previousStatus = getStatus

	def getStatus =
		(workmasters.size, workmasters.values.sum, requests.size, waiting.size, sent.size)

	scheduleTick

	def receive = {
		case wms @ WorkMasterStatus(freeCores, _) =>
			if (! workmasters.contains(sender)) {
				trace(s"New WorkMaster ${sender}, ${freeCores} free cores")
				context.watch(sender)
			}
			trace(s"Updating WorkMaster status for ${sender} to ${freeCores} free cores")
			workmasters.update(sender, freeCores)
			dashboard ! WorkMasterUpdate(sender.path.address, wms)

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				trace(s"WorkMaster ${sender.path} terminated")
				workmasters.remove(dead)
				dashboard ! WorkMasterDown(dead.path.address)
			}

		case RequestManySlots(slots) =>
			trace(s"Received a request for ${slots.length} slots.")
			for (slot <- slots) {
				requests.update(slot, requests.getOrElse(slot, List()) :+ sender())
				slotArchiver ! RequestSingleSlot(slot)
			}
			trace(s"Passed ${slots.length} requests on to the slot archiver")

		case CancelSlots(slots) =>
			trace(s"Received a request for cancelling ${slots.length} slots.")
			val toCancel = slots.toSet
			waiting = waiting.filterNot(toCancel.contains)

		case msg @ SlotCalculated(result) => {
			trace("Got SlotCalculated, sending on to slot archive")
			slotArchiver ! msg
			removeSlot(result.slot)
		}

		case msg @ StiltFailure(slot) =>
			removeSlot(slot)
			removeRequests(slot, msg)

		case msg @ SlotAvailable(local) =>
			removeRequests(local.slot, msg)

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

	private def removeRequests(slot: StiltSlot, msg: Any): Unit = {
		requests.remove(slot) match {
			case None => trace(s"$msg with no requests to match!")
			case Some(actors) => {
				actors.foreach { _ ! msg }
				trace(s"$msg passed on to ${actors.size} actors")
			}
		}
	}

	private def removeSlot(slot: StiltSlot): Unit = {
		waiting = waiting.filter { _ != slot }
		sent.retain { case (_, sentSlot) => sentSlot != slot }
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
				trace(s"Sent slot to ${wm} (timeout in ${calcTimeout})")
				// Keep track of the number of available slot ourselves.
				// This will get overwritten once the workmaster reports in
				// again.
				workmasters.update(wm, i)
			}
		}
	}
}
