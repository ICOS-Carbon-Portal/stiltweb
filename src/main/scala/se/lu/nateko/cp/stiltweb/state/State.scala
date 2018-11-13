package se.lu.nateko.cp.stiltweb.state

import java.nio.file.Path

import scala.collection.mutable.Map
import scala.collection.mutable.Set

import akka.actor.ActorRef
import se.lu.nateko.cp.stiltcluster._

/**
 * Mutable state to be used from an actor.
 * By design, this class itself is not supposed to write anything or send any messages,
 * however, JobState instances that it contains mark jobs as done on the file system.
 */
class State(stateDir: Path, slotStepInMinutes: Int) {

	type Worker = ActorRef
	type JobId = String

	private val slots = new StateOfSlots
	private val cores: Map[Worker, Int] = Map.empty
	private val jobs: Map[JobId, JobState] = Map.empty

	val slotArchiver = new SlotArchiver(stateDir, slotStepInMinutes)

	def isKnownWorker(w: Worker): Boolean = cores.contains(w)

	def handleWorkerUpdate(w: Worker, wms: WorkMasterStatus): Unit = {
		cores.update(w, wms.nCpusTotal)
		slots.registerBeingWorkedOn(w, wms.work)
	}

	def removeWorker(w: Worker): Unit = {
		cores.remove(w)
		slots.handleDeadWorker(w)
	}

	def startJob(jdir: JobDir): Unit = {
		val allSlots = JobMonitor.calculateSlots(jdir.job, slotStepInMinutes)

		val toCalculate: Seq[StiltSlot] = allSlots.filterNot(slotIsAvailable)

		val jstate = new JobState(jdir, allSlots.size, Set(toCalculate:_*))
		jobs.update(jdir.job.id, jstate)
		slots.enqueue(toCalculate)
	}

	def slotIsAvailable(slot: StiltSlot): Boolean = slotArchiver.load(slot).isDefined

	def distributeWork(): Map[Worker, Seq[StiltSlot]] = for((wm, nCores) <- cores) yield {
		//TODO Handle slots that became completed by a concurrent job here
		wm -> slots.sendSlotsToWorker(wm, nCores)
	}

	def cancelJob(id: JobId): Option[JobDir] = jobs.remove(id).map{jstate =>
		slots.cancelSlots(jstate.slots)
		jstate.jdir
	}

	def onSlotDone(slot: StiltSlot): Unit = jobs.values.foreach(_.onSlotCompletion(slot))

	def getDashboardInfo: DashboardInfo = {
		val infra = cores.toSeq.map{case (worker, totalCores) =>
			val usedCores = slots.usedCores(worker)
			WorkerNodeInfo(worker.path.address, totalCores - usedCores, totalCores)
		}
		val (done, notFinished) = jobs.values.toSeq.partition(_.isDone)
		val (running, queue) = notFinished.partition(_.hasBeenRun)
		DashboardInfo(running.map(_.toInfo), done.map(_.toInfo), queue.map(_.jdir.job), infra)
	}
}
