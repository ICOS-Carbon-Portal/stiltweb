package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef

import scala.collection.mutable.Set
import scala.collection.mutable.Map
import akka.actor.Terminated
import akka.actor.Address

class DashboardMaker extends Actor{

	val resources = Map.empty[Address, WorkMasterStatus]
	var queue = Seq.empty[Job]
	var running, done = Seq.empty[JobInfo]

	val subscribers = Set.empty[ActorRef]

	def notifySubscribers(): Unit = {
		val completeInfo = getInfo
		subscribers.foreach(_ ! completeInfo)
	}

	def getInfo = {
		val infra = resources.map{
			case (addr, wms) => WorkerNodeInfo(addr, wms.nCpusFree, wms.nCpusTotal)
		}.toSeq.sortBy(_.address.toString)
		DashboardInfo(running, done, queue, infra)
	}

	def receive = {

		case Subscribe =>
			val publisher = sender()
			subscribers.add(publisher)
			context watch publisher
			publisher ! getInfo

		case Terminated(publisher) =>
			subscribers.remove(publisher)

		case BeginJob(jdir) =>
			queue = queue :+ jdir.job
			notifySubscribers()

		case jinfo@ JobInfo(job, totalSlots, doneSlots) =>
			removeFromQueue(job)
			updateRunning(jinfo)
			notifySubscribers()

		case JobFinished(jinfo) =>
			removeFromRunning(jinfo.job)
			addToDone(jinfo)
			notifySubscribers()

		case CancelJob(id) =>
			if(cancelJob(id)) notifySubscribers()

		case PleaseSendDashboardInfo =>
			sender ! getInfo

		case WorkMasterUpdate(address, wms) =>
			resources.update(address, wms)
			notifySubscribers()

		case WorkMasterDown(address) =>
			resources.remove(address)
			notifySubscribers()
	}

	def removeFromQueue(job: Job): Boolean = if(queue.contains(job)){
		queue = queue.filterNot(_ == job)
		true
	} else false

	def updateRunning(jinfo: JobInfo): Boolean = running.find(_.id == jinfo.id) match {
		case None =>
			running = jinfo +: running
			true

		case Some(oldInfo) if oldInfo != jinfo =>
			running = jinfo +: running.filter(_.id != jinfo.id)
			true

		case _ => false
	}

	def removeFromRunning(job: Job): Boolean = if(running.exists(_.id == job.id)){
		running = running.filter(_.id != job.id)
		true
	} else false

	def addToDone(jinfo: JobInfo): Boolean = {
		done = done :+ jinfo
		true
	}

	def cancelJob(jobId: String): Boolean = {
		if(queue.exists(_.id == jobId)){
			queue = queue.filterNot(_.id == jobId)
			true
		}else if(running.exists(_.id == jobId)){
			running = running.filter(_.id != jobId)
			true
		}else false
	}
}
