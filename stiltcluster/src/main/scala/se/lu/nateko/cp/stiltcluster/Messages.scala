package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate

sealed trait StiltMessage extends java.io.Serializable

case object WorkMasterRegistration extends StiltMessage

case object StopWorkMaster extends StiltMessage

case class CancelJob(id: String) extends StiltMessage

case class JobStatus(id: String, output: IndexedSeq[String], logs: IndexedSeq[String], errors: IndexedSeq[String])

case class StiltJob(
	siteId: String,
	lat: Double,
	lon: Double,
	alt: Double,
	start: LocalDate,
	stop: LocalDate,
	parallelism: Int
){
	def jobId = "job_" + this.hashCode()
}
