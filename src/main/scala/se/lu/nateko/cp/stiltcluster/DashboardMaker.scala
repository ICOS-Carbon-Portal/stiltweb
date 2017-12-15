package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef

import scala.collection.mutable.Set
import akka.actor.Terminated

class DashboardMaker extends Actor{

	var info = DashboardInfo(Nil, Nil, Nil)
	val subscribers = Set.empty[ActorRef]

	def notifySubscribers(): Unit = subscribers.foreach(_ ! info)

	def receive = {

		case Subscribe =>
			val publisher = sender()
			subscribers.add(publisher)
			context watch publisher
			publisher ! info

		case Terminated(publisher) =>
			subscribers.remove(publisher)

		case BeginJob(jdir) =>
			info = info.copy(queue = info.queue :+ jdir.job)
			notifySubscribers()

		case jinfo@ JobInfo(job, totalSlots, doneSlots) =>
			var mustNotify = false

			mustNotify |= removeFromQueue(job)

			if(totalSlots != doneSlots)
				mustNotify |= updateRunning(jinfo)
			else {
				mustNotify |= removeFromRunning(jinfo)
				mustNotify |= addToDone(jinfo)
			}
			if(mustNotify) notifySubscribers()
	}

	def removeFromQueue(job: Job): Boolean = if(info.queue.contains(job)){
		info = info.copy(queue = info.queue.filterNot(_ == job))
		true
	} else false

	def updateRunning(jinfo: JobInfo): Boolean = info.running.find(_.id == jinfo.id) match {
		case None =>
			info = info.copy(running = jinfo +: info.running)
			true

		case Some(oldInfo) if oldInfo != jinfo =>
			val newRunning = jinfo +: info.running.filter(_.id != jinfo.id)
			info = info.copy(running = newRunning)
			true

		case _ => false
	}

	def removeFromRunning(jinfo: JobInfo): Boolean = if(info.running.exists(_.id == jinfo.id)){
		info = info.copy(running = info.running.filter(_.id != jinfo.id))
		true
	} else false

	def addToDone(jinfo: JobInfo): Boolean = {
		info = info.copy(done = info.done :+ jinfo)
		true
	}
}
