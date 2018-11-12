package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.{Map, Queue, Set}
import akka.actor.ActorRef

class StateOfSlots {

	type Worker = ActorRef
	type WorkLevels = Map[Worker, Set[StiltSlot]]


	private val waiting = Queue.empty[StiltSlot]

	private val sentToWorkers: WorkLevels = noWork

	private val beingWorkedOn: WorkLevels = noWork

	private def noWork: WorkLevels = Map.empty

	private def business(worker: Worker, levels: WorkLevels): Int = levels.get(worker).map(_.size).getOrElse(0)


	def enqueue(slots: Seq[StiltSlot]): Unit = waiting ++= slots

	def usedCores(worker: Worker): Int = business(worker, sentToWorkers) + business(worker, beingWorkedOn)

	def sendSlotsToWorker(worker: Worker, totalCores: Int): Seq[StiltSlot] = {

		val freeCores = totalCores - usedCores(worker)

		if(freeCores > 0){

			val slots = for(
				_ <- 1 to freeCores if !waiting.isEmpty
			) yield waiting.dequeue()

			val currentWork = sentToWorkers.getOrElseUpdate(worker, Set.empty[StiltSlot])
			currentWork ++= slots

			slots
		} else Nil

	}

	def registerBeingWorkedOn(worker: Worker, slots: Seq[StiltSlot]): Unit = {
		sentToWorkers.get(worker).foreach{
			currentWork =>
				currentWork --= slots
				enqueue(currentWork.toSeq) //what was sent but did not become worked on; that is, the rejected work
				currentWork.clear()
		}

		val currentWork = beingWorkedOn.getOrElseUpdate(worker, Set.empty[StiltSlot])
		currentWork ++= slots
	}

	def registerCompletion(worker: Worker, slot: StiltSlot): Unit = {
		beingWorkedOn.get(worker).foreach{
			currentWork => currentWork -= slot
		}
	}

	def handleDeadWorker(worker: Worker): Unit = {
		sentToWorkers.remove(worker).foreach{
			slots => enqueue(slots.toSeq)
		}
		beingWorkedOn.remove(worker).foreach{
			slots => enqueue(slots.toSeq)
		}
	}

	def cancelSlots(slots: Seq[StiltSlot]): Unit = {
		val toCancel = slots.toSet
		waiting.dequeueAll(toCancel.contains)
	}
}
