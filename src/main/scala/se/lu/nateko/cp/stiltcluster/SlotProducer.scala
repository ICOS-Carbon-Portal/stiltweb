package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.Map

import akka.actor.{Actor, ActorRef, Terminated}
import akka.actor.Props
import akka.actor.ActorLogging

object SlotProducer{
	def props(archiver: SlotArchiver) = Props.create(classOf[SlotProducer], archiver)
}


class SlotProducer(archiver: SlotArchiver) extends Actor with ActorLogging{

	val dashboard = context.actorSelection("/user/dashboardmaker")

	val workmasters = Map[ActorRef, Int]()         //workmaster -> totalCores
	val requests = Map[StiltSlot, Seq[ActorRef]]() //slot       -> job monitors that want it
	val state = new StateOfSlots

	def receive: Receive = basicReceive.andThen{_ =>
		sendSomeSlots()
	}

	def basicReceive: Receive = {
		case wms @ WorkMasterStatus(totalCores, _) =>
			if (! workmasters.contains(sender)) {
				log.info(s"Seeing new computational node $sender with $totalCores CPUs")
				workmasters.update(sender, totalCores)
				context.watch(sender)
			}
			dashboard ! WorkMasterUpdate(sender.path.address, wms)

		case Terminated(dead) =>
			if (workmasters.contains(dead)) {
				log.info(s"Computational node at ${sender.path} terminated")
				workmasters.remove(dead)
				state.handleDeadWorker(dead)
				dashboard ! WorkMasterDown(dead.path.address)
			}

		case RequestManySlots(slots) =>
			log.debug(s"Received a request for ${slots.length} slots.")

			val toCalculate = slots.map{slot => archiver.load(slot) match {
				case Some(local) =>
					sender() ! local
					None
				case None =>
					requests.update(slot, requests.getOrElse(slot, List()) :+ sender())
					Some(slot)
			}}.flatten
			state.enqueue(toCalculate)

		case CancelSlots(slots) =>
			log.debug(s"Received a request for cancelling ${slots.length} slots.")
			state.cancelSlots(slots)

		case SlotCalculated(result) => {
			log.debug("Got SlotCalculated, saving to the slot archive")
			val local = archiver.save(result)
			state.registerCompletion(sender(), result.slot)
			removeRequests(result.slot, local)
		}

		case msg @ StiltFailure(slot) =>
			state.registerCompletion(sender(), slot)
			removeRequests(slot, msg)

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

	private def sendSomeSlots(): Unit = for((wm, nCores) <- workmasters){

		val slots = state.sendToWorker(wm, nCores)

		if(!slots.isEmpty){
			wm ! CalculateSlots(slots)
			log.debug(s"Sent ${slots.size} slots to ${wm}")
		}
	}
}
