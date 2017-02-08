package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import java.time.LocalDate

class WorkReceptionist extends Actor{
	import WorkReceptionist._

	private var nodes = IndexedSeq.empty[ActorRef]
	val log = context.system.log

	def receive = {
		case WorkMasterRegistration if ! nodes.contains(sender()) =>
			context watch sender()
			nodes = nodes :+ sender()
			log.info("WORK MASTER REGISTERED: " + sender())

			sender() ! testJob

		case run: JobRun =>
			log.info("STARTED STILT RUN: " + run.toString)

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