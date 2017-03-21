package se.lu.nateko.cp.stiltcluster

import akka.actor.Address
import scala.collection.immutable.Seq

case object Subscribe

case class JobInfo(run: JobRun, status: JobStatus, executionNode: Address){
	def finished = status.exitValue.isDefined
	def id = status.id
}

case class DashboardInfo(running: Seq[JobInfo], done: Seq[JobInfo], queue: Seq[Job]){

	def findCancellableJobById(jobId: String): Option[Job] = {
		queue.find(_.id == jobId).orElse(running.find(_.id == jobId).map(_.run.job))
	}
}


case object PleaseSendDashboardInfo
