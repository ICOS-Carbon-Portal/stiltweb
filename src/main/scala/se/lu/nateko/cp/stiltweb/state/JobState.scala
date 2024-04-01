package se.lu.nateko.cp.stiltweb.state

import scala.collection.mutable.Buffer
import scala.collection.mutable.Set

import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.JobInfo
import se.lu.nateko.cp.stiltcluster.SlotFailure
import se.lu.nateko.cp.stiltcluster.StiltSlot
import java.time.Instant

class JobState(val job: Job, nSlotsTotal: Int, initWork: Seq[StiltSlot]){

	val initRemainingSlots = initWork.size
	private[this] val slotz = Set(initWork: _*)
	private[this] val failures = Buffer.empty[SlotFailure]
	private[this] var doneTime: Option[Instant] = None

	def slots = slotz.toSeq
	def nSlots = slotz.size

	def isDone() = {
		val done = slotz.isEmpty
		if(done && doneTime.isEmpty) doneTime = Some(Instant.now)
		done
	}

	def isFinishedBy(slot: StiltSlot): Boolean = slotz.remove(slot) && isDone()

	def toInfo = {
		val jobWithStop = if(doneTime.isDefined) job.copy(timeStopped = doneTime) else job
		JobInfo(jobWithStop, nSlotsTotal, nSlotsTotal - slotz.size, None, failures.toSeq)
	}
	def hasBeenRun = slotz.size < initRemainingSlots

	def rememberFailuresIfRelevant(slot: StiltSlot, errMsgs: Seq[String], logsPathMaker: Job => String): Option[Job] =
		if(slotz.contains(slot)){
			failures += SlotFailure(slot, errMsgs, logsPathMaker(job))
			Some(job)
		} else None
}
