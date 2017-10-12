package se.lu.nateko.cp.stiltcluster

import akka.actor.{Actor, Props,ActorLogging}




class WorkReceptionist extends Actor with ActorLogging {

	val jobArchiver = context.actorSelection("/user/jobarchiver")
	val slotProducer = context.actorSelection("/user/slotproducer")

	def receive = {
		case job: Job =>
			log.info("WorkReceptionist receiving job")
			jobArchiver ! PersistJob(job)

		case BeginJob(job, dir) =>
			log.info(s"Starting new job $job")
			context.actorOf(Props(new JobMonitor(job)), name="jobmonitor")

		case msg: WorkMasterStatus =>
			slotProducer ! msg

		case msg: SlotCalculated =>
			slotProducer ! msg
	}
}
