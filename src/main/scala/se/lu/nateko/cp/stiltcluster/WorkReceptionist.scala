package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated

class WorkReceptionist extends Actor{

	private var workers = IndexedSeq.empty[ActorRef]

	def receive = {
		case WorkMasterRegistration if ! workers.contains(sender()) =>
			context watch sender()
			workers = workers :+ sender()
			context.system.log.info("WORKER REGISTERED: " + sender())

		case Terminated(w) =>
			context.system.log.info("WORKER UNREGISTERED: " + sender())
			workers = workers.filter(_ != w)
	}
}