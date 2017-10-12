package se.lu.nateko.cp.stiltcluster

import java.nio.file.Path


case object Subscribe

case class PersistJob(job: Job)
case class BeginJob(job: Job, dir: String)

case class CalculateSlots(job: Job)
case class SlotsCalculated(job: Job, slots: Seq[String])

case object JobMonitorRegistering
case object SendSlotRequest
case object AllDone

case class JobFinished(job: Job)
case class SlotRequest(job: Job, slot: String)
case class SlotAvailable(job: Job, slot: String, data: Path)
case class SlotUnAvailable(job: Job, slot: String)
case class LinkAvailableSlots(job: Job, dir: String, slots: Seq[String])

case class JobInfo(job: Job, nSlots: Int, nSlotsFinished: Int) {
	def id = job.id
}

case class DashboardInfo(running: Seq[JobInfo], done: Seq[JobInfo], queue: Seq[Job]){

	def findCancellableJobById(jobId: String): Option[Job] = {
		queue.find(_.id == jobId).orElse(running.find(_.id == jobId).map(_.job))
	}
}


case object PleaseSendDashboardInfo
