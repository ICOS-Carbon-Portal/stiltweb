package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import java.time.LocalDate
import scala.collection.mutable.Map

class WorkReceptionist extends Actor{
	import WorkReceptionist._

	private var nodes = IndexedSeq.empty[ActorRef]
	private val status = Map.empty[ActorRef, WorkMasterStatus]

	val log = context.system.log

	def receive = {

		case StopAllWork =>
			nodes.foreach(_ ! StopAllWork)

		case s: WorkMasterStatus =>
			status += ((sender(), s))

		case run: JobRun =>
			val oldStatus = status(sender())
			val newStatus = oldStatus.copy(
				work = oldStatus.work :+ ((run, JobStatus.init(run.runId)))
			)
			status += ((sender(), newStatus))
			log.info("STARTED STILT RUN: " + run.toString)

		case WorkMasterRegistration(nCores) if ! nodes.contains(sender()) =>
			context watch sender()
			nodes = nodes :+ sender()
			status += ((sender(), WorkMasterStatus(Nil, nCores)))

			log.info("WORK MASTER REGISTERED: " + sender())
			log.info("CURRENT WORK MASTERs: " + nodes.mkString(", "))

		case Terminated(w) =>
			log.info("WORK MASTER UNREGISTERED: " + sender())
			nodes = nodes.filter(_ != w)
	}
}

object WorkReceptionist{
	import java.time.temporal.ChronoUnit

	/**
	 * Cost of the job, in number of footprints
	 */
	def cost(job: Job): Int = {
		val days = (job.start.until(job.stop, ChronoUnit.DAYS) + 1).toInt
		days * 8 //1 footprints per 3 hours
	}

	val testJob = Job(
		siteId = "HTM",
		lat = 56.1,
		lon = 13.42,
		alt = 150,
		start = LocalDate.parse("2012-06-15"),
		stop = LocalDate.parse("2012-06-16")
	)
}
