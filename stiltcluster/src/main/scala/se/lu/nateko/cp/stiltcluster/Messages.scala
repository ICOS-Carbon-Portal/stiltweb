package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate

case object WorkMasterRegistration

case object StopWorkMaster

case class CancelJob(id: String)

case object GetStatus

case class JobStatus(
	id: String,
	exitValue: Option[Int],
	output: Seq[String],
	logs: Seq[String],
	errors: Seq[String]
)

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
