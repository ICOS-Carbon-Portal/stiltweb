package se.lu.nateko.cp.stiltweb.state

import scala.collection.mutable.Set
import se.lu.nateko.cp.stiltcluster.JobDir
import se.lu.nateko.cp.stiltcluster.StiltSlot
import se.lu.nateko.cp.stiltcluster.JobInfo

class JobState(val jdir: JobDir, nSlotsTotal: Int, slotz: Set[StiltSlot]){
	markAsDoneIfDone()

	val initRemainingSlots = slotz.size

	def slots = slotz.toSeq
	def isDone = slotz.isEmpty
	def markAsDoneIfDone(): Unit = if(isDone) jdir.markAsDone()

	def onSlotCompletion(slot: StiltSlot): Unit = {
		slotz -= slot
		markAsDoneIfDone()
	}

	def toInfo = JobInfo(jdir.job, nSlotsTotal, nSlotsTotal - slotz.size)
	def hasBeenRun = slotz.size < initRemainingSlots
}
