package se.lu.nateko.cp.stiltcluster

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage._
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.Props

class DashboardPublisher(receptionist: ActorRef) extends ActorPublisher[DashboardInfo]{

	private var unsentInfo: DashboardInfo = null

	override def preStart(): Unit = {
		receptionist ! Subscribe
		context watch receptionist
	}

	override def receive = {

		case di: DashboardInfo =>
			if(totalDemand > 0 && isActive) {
				onNext(di)
				unsentInfo = null
			} else
				unsentInfo = di

		case Cancel | SubscriptionTimeoutExceeded =>
			receptionist ! Cancel
			context stop self

		case Request(n) =>
			if(n > 0 && unsentInfo != null) {
				onNext(unsentInfo)
				unsentInfo = null
			}

		case Terminated(watchee) =>
			if(watchee == receptionist) onComplete()

	}

}


object DashboardPublisher{

	def props(receptionist: ActorRef) = Props(classOf[DashboardPublisher], receptionist)

}
