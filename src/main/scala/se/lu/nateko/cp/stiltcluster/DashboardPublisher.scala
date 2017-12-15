package se.lu.nateko.cp.stiltcluster

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage._
import akka.actor.ActorRef
import akka.actor.Props

class DashboardPublisher(dashboard: ActorRef) extends ActorPublisher[DashboardInfo]{

	private var unsentInfo: DashboardInfo = null

	override def preStart(): Unit = {
		dashboard ! Subscribe
	}

	override def receive = {

		case di: DashboardInfo =>
			if(totalDemand > 0 && isActive) {
				onNext(di)
				unsentInfo = null
			} else
				unsentInfo = di

		case Cancel | SubscriptionTimeoutExceeded =>
			context stop self

		case Request(n) =>
			if(n > 0 && unsentInfo != null) {
				onNext(unsentInfo)
				unsentInfo = null
			}

	}

}


object DashboardPublisher{

	def props(dashboard: ActorRef) = Props(classOf[DashboardPublisher], dashboard)

}
