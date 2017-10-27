package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, Props,ActorLogging}

class WorkReceptionist extends Actor with ActorLogging {

	val jobArchiver = context.actorSelection("/user/jobarchiver")

	def receive = {
		case job: Job =>
			log.info("WorkReceptionist receiving job")
			jobArchiver ! PersistJob(job)

		case BeginJob(jdir) =>
			log.info(s"Starting new job $jdir.job")
			context.actorOf(Props(new JobMonitor(jdir)), name="jobmonitor")

	}
}
