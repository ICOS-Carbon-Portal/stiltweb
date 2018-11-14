package se.lu.nateko.cp.stiltweb.state

import scala.collection.mutable.Set
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltSlot
import se.lu.nateko.cp.stiltcluster.JobInfo

class JobState(val job: Job, nSlotsTotal: Int, initWork: Seq[StiltSlot]){

	val initRemainingSlots = initWork.size
	private[this] val slotz = Set(initWork: _*)

	def slots = slotz.toSeq
	def isDone = slotz.isEmpty

	def isFinishedBy(slot: StiltSlot): Boolean = slotz.remove(slot) && isDone

	def toInfo = JobInfo(job, nSlotsTotal, nSlotsTotal - slotz.size)
	def hasBeenRun = slotz.size < initRemainingSlots
}
