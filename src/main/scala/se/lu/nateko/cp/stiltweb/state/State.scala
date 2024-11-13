package se.lu.nateko.cp.stiltweb.state

import scala.collection.mutable.Map
import scala.collection.mutable.Queue
import scala.collection.mutable.Set

import akka.actor.ActorRef
import se.lu.nateko.cp.stiltcluster._
import java.time.Instant

/**
 * Mutable state to be used from an actor (so, needs not be thread-safe).
 * By design, this class is not supposed to write to "outer world" or send any messages,
 * but can mutate itself and its members.
 */
class State(archiver: Archiver):

	type Worker = ActorRef
	type JobId = String

	private val slots = Queue.empty[StiltSlot]
	private val workers = Map.empty[Worker, WorkmasterState]
	private val jobs = Map.empty[JobId, JobState]

	def isKnownWorker(w: Worker): Boolean = workers.contains(w)

	/**
	 * returns the lost work
	 */
	def handleWorkerUpdate(w: Worker, wms: WorkMasterStatus): Seq[StiltSlot] = workers.get(w) match {
		case Some(state) =>
			val lostWork = state.updateAndGetLostWork(wms)
			enqueue(lostWork)
			lostWork
		case None =>
			workers.update(w, new WorkmasterState(wms))
			Nil
	}

	def removeWorker(w: Worker): Unit = workers.remove(w).foreach{wstate =>
		enqueue(wstate.unfinishedWork)
	}

	/**
	 * returns true if there is actual work to do, false otherwise
	 */
	def startJob(job: Job): Boolean = {
		val allSlots = archiver.calculateSlots(job)

		val toCalculate: Seq[StiltSlot] = allSlots.filterNot(slotIsAvailable)

		val jstate = new JobState(job.copy(timeStarted = Some(Instant.now)), allSlots.size, toCalculate)
		jobs.update(job.id, jstate)
		enqueue(toCalculate)
		!toCalculate.isEmpty
	}

	def enqueue(work: Seq[StiltSlot]): Unit = slots ++= work

	def slotIsAvailable(slot: StiltSlot): Boolean = archiver.load(slot).isDefined

	def distributeWork(): Map[Worker, CalculateSlots] = workers
		.collect{
			case (wm, wstate) if wstate.freeCores > 0 && !wstate.isBadWorker && !slots.isEmpty =>
				val work = grabWork(wstate.freeCores)
				val requestId = wstate.requestWork(work)
				wm -> CalculateSlots(requestId, work)
		}

	private def grabWork(maxNumSlots: Int): Seq[StiltSlot] = {
		val buff = Set.empty[StiltSlot]

		while(!slots.isEmpty && buff.size < maxNumSlots){
			val next = slots.dequeue()
			if(!slotIsAvailable(next)) buff += next
		}
		buff.toSeq
	}

	def cancelJob(id: JobId): Option[Job] = jobs.remove(id).map{jstate =>
		val toRemove = Set(jstate.slots: _*)
		//the following will remove only one occurrence of slot per queue
		//this is desired because same slot may be repeated due to overlapping jobs
		slots.dequeueAll(toRemove.remove)

		jstate.job
	}

	/**
	 * @return jobs completed as a result of this slot completion
	 */
	def onSlotDone(w: Worker, slot: StiltSlot): Seq[Job] = {
		workers.get(w).foreach(_.onSlotDone(slot))
		jobs.values.collect{
			case jstate if jstate.isFinishedBy(slot) => jstate.job
		}.toSeq
	}

	/**
	 * Records the failure.
	 * @return incomplete jobs that include this slot
	 */
	def onSlotFailure(slot: StiltSlot, errMsgs: Seq[String], logsPathMaker: Job => String): Seq[Job] =
		jobs.values.flatMap{_.rememberFailuresIfRelevant(slot, errMsgs, logsPathMaker)}.toSeq

	def getDashboardInfo: DashboardInfo =
		val infra = workers.toSeq.map{case (worker, wstate) =>
			WorkerNodeInfo(worker.path.address, wstate.freeCores, wstate.totalCores, wstate.isBadWorker)
		}
		val (done, notFinished) = jobs.values.toSeq.partition(_.isDone())
		val (running, queue) = notFinished.partition(_.hasBeenRun)

		val leftMinutes: Option[Int] =
			val totalCores = infra.map(_.nCpusTotal).sum
			if totalCores == 0 then None else Some:
				val totalSlots = running.map(_.nSlots).sum
				val nBatches = (totalSlots + totalCores - 1) / totalCores // int div with roundup
				val nSeconds = nBatches * 95
				(nSeconds + 60 - 1) / 60 // int div with roundup

		DashboardInfo(
			running.map(_.toInfo.copy(minutesRemaining = leftMinutes))
				.sortBy(_.job.timeStarted),
			done.map(_.toInfo).sortBy(_.job.timeStopped).reverse,
			queue.map(_.toInfo).sortBy(_.job.timeStarted),
			infra
		)
end State
