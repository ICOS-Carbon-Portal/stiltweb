package se.lu.nateko.cp.stiltcluster

import java.time.{Instant, LocalDate}


/** The description of a Stilt simulation to be run. */
case class Job(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Int,
	start: LocalDate,
	stop: LocalDate,
	userId: String,
	timeEnqueued: Option[Instant] = None,
	timeStarted: Option[Instant] = None,
	timeStopped: Option[Instant] = None
){
	def id = "job_" + this.copy(timeEnqueued = None, timeStarted = None, timeStopped = None).hashCode()

	def copySetEnqueued =
		this.copy(timeEnqueued=Some(Instant.now()))

	def copySetStarted =
		this.copy(timeStarted=(Some(Instant.now())))

	def copySetStopped =
		this.copy(timeStopped=Some(Instant.now()))
}


case class PersistJob(job: Job)
case class BeginJob(jdir: JobDir)

case class CalculateSlotList(job: Job)
case class SlotListCalculated(slots: Seq[StiltSlot])

case class RequestManySlots(slots: Seq[StiltSlot])
case class RequestSingleSlot(slot: StiltSlot)
case class SlotAvailable(slot: LocallyAvailableSlot)
case class SlotUnAvailable(slot: StiltSlot)

case object Subscribe
case object JobMonitorRegistering
case object SendSlotRequest
case object AllDone

case class JobFinished(jdir: JobDir)


case class JobInfo(job: Job, nSlots: Int, nSlotsFinished: Int) {
	def id = job.id
}

case class DashboardInfo(running: Seq[JobInfo], done: Seq[JobInfo], queue: Seq[Job]){

	def findCancellableJobById(jobId: String): Option[Job] = {
		queue.find(_.id == jobId).orElse(running.find(_.id == jobId).map(_.job))
	}
}


case object PleaseSendDashboardInfo
