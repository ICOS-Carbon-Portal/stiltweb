package se.lu.nateko.cp.stiltweb.state

import scala.collection.mutable.Buffer
import scala.collection.mutable.Set

import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.JobInfo
import se.lu.nateko.cp.stiltcluster.SlotFailure
import se.lu.nateko.cp.stiltcluster.StiltSlot

class JobState(val job: Job, nSlotsTotal: Int, initWork: Seq[StiltSlot]){

	val initRemainingSlots = initWork.size
	private[this] val slotz = Set(initWork: _*)
	private[this] val failures = Buffer.empty[SlotFailure]

	def slots = slotz.toSeq
	def isDone = slotz.isEmpty

	def isFinishedBy(slot: StiltSlot): Boolean = slotz.remove(slot) && isDone

	def toInfo = {
		JobInfo(job, nSlotsTotal, nSlotsTotal - slotz.size, failures)
	}
	def hasBeenRun = slotz.size < initRemainingSlots

	def rememberFailuresIfRelevant(slot: StiltSlot, errMsgs: Seq[String], logsPathMaker: Job => String): Option[Job] =
		if(slotz.contains(slot)){
			failures += SlotFailure(slot, errMsgs, logsPathMaker(job))
			Some(job)
		} else None
}
