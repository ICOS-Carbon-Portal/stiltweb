package se.lu.nateko.cp.stiltcluster

import java.time.{ Instant, LocalDate }
import scala.collection.immutable.Seq

case object Hi

case class CancelJob(id: String)

case class JobCanceled(id: String)

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

case class CalculateSlot(job: Job, slot: String)
case class SimulationComplete(job: Job, slot: String, outputDir: String)
case class WorkMasterStatus(nCpusFree: Int)
case class Thanks(ids: Seq[String])
case class Slot(ob: Job, id: String)
//case class SlotCalculated(job: Job, dir: String, slot: String, blob: Array[Byte])
case class SlotCalculated(job: Job, slot: String)
