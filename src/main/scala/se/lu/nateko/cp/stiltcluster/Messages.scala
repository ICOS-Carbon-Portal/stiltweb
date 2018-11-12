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


case class BeginJob(jdir: JobDir)

case class RequestManySlots(slots: Seq[StiltSlot])
case class CancelSlots(slots: Seq[StiltSlot])

case object Subscribe

case class JobFinished(jinfo: JobInfo)

case class JobInfo(job: Job, nSlots: Int, nSlotsFinished: Int) {
	def id = job.id
}

case class WorkerNodeInfo(address: Address, nCpusFree: Int, nCpusTotal: Int)

case class DashboardInfo(
		running: Seq[JobInfo],
		done: Seq[JobInfo],
		queue: Seq[Job],
		infra: Seq[WorkerNodeInfo]
	){

	def findCancellableJobById(jobId: String): Option[Job] = {
		queue.find(_.id == jobId).orElse(running.find(_.id == jobId).map(_.job))
	}
}

case object PleaseSendDashboardInfo

case class WorkMasterUpdate(address: Address, status: WorkMasterStatus)
case class WorkMasterDown(address: Address)
