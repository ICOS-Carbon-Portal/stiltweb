package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, Props,ActorLogging}

class WorkReceptionist extends Actor with ActorLogging {

	val jobArchiver = context.actorSelection("/user/jobarchiver")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	def receive = {
		case job: Job =>
			log.info("Receiving job, sending it to the jobarchiver")
			jobArchiver ! PersistJob(job)

		case beginning @ BeginJob(jdir) =>
			log.info(s"Starting new job $jdir.job")
			context.actorOf(Props(new JobMonitor(jdir)))
			dashboard ! beginning

	}
}
