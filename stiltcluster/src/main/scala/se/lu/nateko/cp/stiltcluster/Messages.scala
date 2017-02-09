package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate

case class WorkMasterRegistration(nCores: Int)

case object StopAllWork

case class CancelJob(id: String)

case object GetStatus
case object CollectStatus

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

case class JobCanceled(status: JobStatus)

case class Job(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Int,
	start: LocalDate,
	stop: LocalDate
)

case class JobRun(job: Job, parallelism: Int){
	def runId = "job_" + this.hashCode()
}

case class WorkMasterStatus(work: Seq[(JobRun, JobStatus)], freeCores: Int)


case class Thanks(ids: Seq[String])
