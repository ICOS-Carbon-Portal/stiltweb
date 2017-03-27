package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate
import java.time.Instant
import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer

case object Hi

case object StopAllWork

case class CancelJob(id: String)

case class JobStatus(
	id: String,
	exitValue: Option[Int],
	output: Seq[String],
	logs: Seq[String],
	errors: Seq[String]
)

object JobStatus{
	def init(id: String) = JobStatus(id, None, Nil, Nil, Nil)
}

case class JobCanceled(id: String)

case class LogEntry(what: String, when: String)

case class Job(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Int,
	start: LocalDate,
	stop: LocalDate,
	userId: String,

	logbook: ListBuffer[LogEntry]
){
	def id = "job_" + this.hashCode()

	def add_logbook_entry(what: String): Unit = {
		logbook += LogEntry(what, java.time.Instant.now.toString)
	}
}

case class JobRun(job: Job, parallelism: Int)

case class WorkMasterStatus(work: Seq[(JobRun, JobStatus)], freeCores: Int){
	def isRunning(job: Job): Boolean = work.exists{
		case (JobRun(running, _), _) => running == job
	}
}


case class Thanks(ids: Seq[String])
