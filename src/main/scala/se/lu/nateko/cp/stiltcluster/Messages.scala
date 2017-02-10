package se.lu.nateko.cp.stiltcluster

import akka.actor.Address

case class JobInfo(run: JobRun, status: JobStatus, executionNode: Address){
	def finished = status.exitValue.isDefined
}
case class DashboardInfo(running: Seq[JobInfo], done: Seq[JobInfo], queue: Seq[Job])

case class StiltResultsRequest(stationId: String, year: Int, columns: Seq[String])

