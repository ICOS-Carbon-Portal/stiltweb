package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}
import akka.actor.Props
import java.nio.file.Path

class WorkReceptionist(stateDir: Path, slotStepInMinutes: Integer) extends Actor with ActorLogging {

	val jobsDir = Util.ensureDirectory(stateDir.resolve("jobs"))
	val dashboard = context.actorSelection("/user/dashboardmaker")

	override def preStart() = {
		log.info(s"Starting up, looking in ${jobsDir} for unfinished jobs")
		findUnfinishedJobs.foreach(jdir => self ! BeginJob(jdir))
	}

	def receive = {
		case jobRequest: Job =>
			log.info(s"Receiving job, saving it to $jobsDir")
			val job = jobRequest.copySetStarted
			val jdir = JobDir.save(job, jobsDir.resolve(job.id))
			self ! BeginJob(jdir)

		case beginning @ BeginJob(jdir) =>
			log.info(s"Starting new job $jdir.job")
			context.actorOf(JobMonitor.props(jdir, slotStepInMinutes))
			dashboard ! beginning

		case deletion: CancelJob =>
			context.children foreach{_ ! deletion}
	}

	def findUnfinishedJobs: Iterator[JobDir] = {
		val pathIter = Util.iterateChildren(jobsDir)
		for(dir <- pathIter if JobDir.isUnfinishedJobDir(dir) ) yield JobDir(dir)
	}
}

object WorkReceptionist{
	def props(stateDir: Path, slotStepInMinutes: Integer) = Props.create(classOf[WorkReceptionist], stateDir: Path, slotStepInMinutes)
}
