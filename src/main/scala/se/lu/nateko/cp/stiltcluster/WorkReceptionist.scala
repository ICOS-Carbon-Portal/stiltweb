package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}
import java.nio.file.Path
import akka.actor.Props

class WorkReceptionist(mainDirectory: Path) extends Actor with ActorLogging {

	val jobArchiver = context.actorSelection("/user/jobarchiver")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	def receive = {
		case job: Job =>
			log.info("Receiving job, sending it to the jobarchiver")
			jobArchiver ! PersistJob(job.copySetStarted)

		case beginning @ BeginJob(jdir) =>
			log.info(s"Starting new job $jdir.job")
			context.actorOf(JobMonitor.props(jdir, mainDirectory))
			dashboard ! beginning

		case deletion: CancelJob =>
			context.children foreach{_ ! deletion}
	}
}

object WorkReceptionist{
	def props(mainDirectory: Path) = Props.create(classOf[WorkReceptionist], mainDirectory)
}
