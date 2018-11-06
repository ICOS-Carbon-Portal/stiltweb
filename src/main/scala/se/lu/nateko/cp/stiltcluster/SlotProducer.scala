package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Set}
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Terminated}
import akka.actor.Props
import akka.actor.ActorLogging
import scala.collection.mutable.Queue

object SlotProducer{
	def props(archiver: SlotArchiver) = Props.create(classOf[SlotProducer], archiver)
}


class SlotProducer(archiver: SlotArchiver) extends Actor with ActorLogging{

	//TODO Replace timeout with DeathWatch on work masters
	final val calcTimeout = 400 seconds

	val dashboard = context.actorSelection("/user/dashboardmaker")

	val workmasters = Map[ActorRef, Int]()         //workmaster -> freeCores
	val requests = Map[StiltSlot, Seq[ActorRef]]() //slot       -> job monitors that want it
	val waiting = Queue.empty[StiltSlot]
	val sent = Set[(Deadline, StiltSlot)]()

	var previousStatus = getStatus

	def getStatus =
		(workmasters.size, workmasters.values.sum, requests.size, waiting.size, sent.size)

	def receive: Receive = basicReceive.andThen{_ =>
		val status = getStatus
		if (status != previousStatus) {
			val (nWm, nCpus, nR, nW, nS) = status
			// Only trace when something has changed, to avoid spamming the log
			log.debug(s"Tick - $nWm workmasters (${nCpus} cpus), $nR requests, $nW waiting, $nS sent")
			previousStatus = status
		}
		checkTimeouts()
		sendSomeSlots()
	}

	def basicReceive: Receive = {
		case wms @ WorkMasterStatus(freeCores, totalCores) =>
			if (! workmasters.contains(sender)) {
				log.info(s"Seeing new computational node $sender with $totalCores CPUs")
				context.watch(sender)
			}
			log.debug(s"Updating WorkMaster status for ${sender} to ${freeCores} free cores")
			workmasters.update(sender, freeCores)
			dashboard ! WorkMasterUpdate(sender.path.address, wms)

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				log.info(s"Computational node at ${sender.path} terminated")
				workmasters.remove(dead)
				dashboard ! WorkMasterDown(dead.path.address)
			}

		case RequestManySlots(slots) =>
			log.debug(s"Received a request for ${slots.length} slots.")
			for (slot <- slots) {
				requests.update(slot, requests.getOrElse(slot, List()) :+ sender())
				handleSlotRequest(slot)
			}

		case CancelSlots(slots) =>
			log.debug(s"Received a request for cancelling ${slots.length} slots.")
			val toCancel = slots.toSet
			waiting.dequeueAll(toCancel.contains)

		case SlotCalculated(result) => {
			log.debug("Got SlotCalculated, saving to the slot archive")
			val local = archiver.save(result)
			removeSlot(result.slot)
			removeRequests(result.slot, local)
		}

		case msg @ StiltFailure(slot) =>
			removeSlot(slot)
			removeRequests(slot, msg)

	}

	private def handleSlotRequest(slot: StiltSlot): Unit = {
		// Before queueing the slot, check that it isn't already available
		archiver.load(slot) match{
			case Some(local) =>
				removeRequests(local.slot, local)
			case None =>
				waiting += slot
		}
	}

	private def removeRequests(slot: StiltSlot, msg: Any): Unit = {
		requests.remove(slot) match {
			case None => log.warning(s"$msg with no requests to match!")
			case Some(actors) => {
				actors.foreach { _ ! msg }
				log.debug(s"$msg passed on to ${actors.size} actors")
			}
		}
	}

	private def removeSlot(slot: StiltSlot): Unit = {
		waiting.dequeueAll( _ === slot)
		sent.retain { case (_, sentSlot) => sentSlot != slot }
	}

	private def checkTimeouts() = {
		val overdue = sent.filter { case (deadline, slot) => deadline.isOverdue }
		overdue.foreach { case elem @ (deadline, slot) =>
			sent.remove(elem)
			if (! requests.contains(slot)) {
				log.debug(s"Overdue slot ${slot} no longer requested")
			} else {
				log.debug(s"Overdue slot ${slot} still requested, bouncing off slot archiver")
				handleSlotRequest(slot)
			}
		}
	}

	private def sendSomeSlots():Unit = for (
		(wm, freeCores) <- workmasters;
		i <- freeCores-1 to 0 by -1;
		slot <- waiting.dequeueFirst(_ => true)
	){
		sent.add((calcTimeout.fromNow, slot))
		wm ! CalculateSlot(slot)
		log.debug(s"Sent slot to ${wm} (timeout in ${calcTimeout})")
		// Keeping track of the number of available slot ourselves.
		// This will get overwritten once the workmaster reports in again.
		workmasters.update(wm, i)
	}
}
