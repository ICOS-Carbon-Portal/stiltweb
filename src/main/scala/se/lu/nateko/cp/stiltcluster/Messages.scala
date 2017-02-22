package se.lu.nateko.cp.stiltcluster

import akka.actor.Address
import scala.collection.immutable.Seq

case object Subscribe

case class JobInfo(run: JobRun, status: JobStatus, executionNode: Address){
	def finished = status.exitValue.isDefined
}
case class DashboardInfo(running: Seq[JobInfo], done: Seq[JobInfo], queue: Seq[Job])

