package se.lu.nateko.cp.stiltcluster

import java.time.{Instant, LocalDate}
import akka.actor.Address


/** The description of a Stilt simulation to be run. */
case class Job(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Int,
	start: LocalDate,
	stop: LocalDate,
	userId: String,
	timeStarted: Option[Instant] = None,
	timeStopped: Option[Instant] = None,
){
	def id = "job_" + this.copy(timeStarted = None, timeStopped = None).hashCode()

	def pos = StiltPosition(lat, lon, alt)

	def copySetStarted =
		this.copy(timeStarted=(Some(Instant.now())))

	def copySetStopped =
		this.copy(timeStopped=Some(Instant.now()))
}

case class CancelJob(id: String)

case class SlotFailure(slot: StiltSlot, errorMessages: Seq[String], logsFilename: String)

case class JobInfo(job: Job, nSlots: Int, nSlotsFinished: Int, failures: Seq[SlotFailure] = Nil) {
	def id = job.id
}

case class WorkerNodeInfo(address: Address, nCpusFree: Int, nCpusTotal: Int)

case class DashboardInfo(
		running: Seq[JobInfo],
		done: Seq[JobInfo],
		queue: Seq[JobInfo],
		infra: Seq[WorkerNodeInfo]
	){

	def findCancellableJobById(jobId: String): Option[Job] = {
		(running ++ queue).find(_.id == jobId).map(_.job)
	}
}

case object PleaseSendDashboardInfo
case object Subscribe
case object DistributeWork
