package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, ActorLogging}
import akka.actor.Props

class WorkReceptionist(slotStepInMinutes: Integer) extends Actor with ActorLogging {

	val jobArchiver = context.actorSelection("/user/jobarchiver")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	def receive = {
		case job: Job =>
			log.info("Receiving job, sending it to the jobarchiver")
			jobArchiver ! PersistJob(job.copySetStarted)

		case beginning @ BeginJob(jdir) =>
			log.info(s"Starting new job $jdir.job")
			context.actorOf(JobMonitor.props(jdir, slotStepInMinutes))
			dashboard ! beginning

		case deletion: CancelJob =>
			context.children foreach{_ ! deletion}
	}
}

object WorkReceptionist{
	def props(slotStepInMinutes: Integer) = Props.create(classOf[WorkReceptionist], slotStepInMinutes)
}
