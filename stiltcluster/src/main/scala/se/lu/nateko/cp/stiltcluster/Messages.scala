package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate
import scala.collection.immutable.Seq

case object Hi

case object StopAllWork

case class CancelJob(id: String)


/** Tracks the status of a Stilt process that is currently executing (i.e as
  * a Unix program) . */
case class ExecutionStatus(
	id: String,
	exitValue: Option[Int],
	output: Seq[String],
	logs: Seq[String],
	errors: Seq[String]
)

object ExecutionStatus{
	def init(id: String) = ExecutionStatus(id, None, Nil, Nil, Nil)
}

case class JobCanceled(id: String)

/** The description of a Stilt simulation to be run. */
case class Job(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Int,
	start: LocalDate,
	stop: LocalDate,
	userId: String
){
	def id = "job_" + this.hashCode()
}

case class JobRun(job: Job, parallelism: Int)

case class WorkMasterStatus(work: Seq[(Job, ExecutionStatus)], freeCores: Int){
	def isRunning(query: Job): Boolean = work.exists{
		case (job, status) => job == query && ! status.exitValue.isDefined
	}
}


case class Thanks(ids: Seq[String])
