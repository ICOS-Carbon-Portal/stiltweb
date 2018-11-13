package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef

import scala.collection.mutable.Set
import akka.actor.Terminated

trait StreamPublisher[T] extends Actor {

	private val subscribers = Set.empty[ActorRef]

	private def notifySubscribers(): Unit = {
		val elem = getStreamElement
		subscribers.foreach(_ ! elem)
	}

	def getStreamElement: T
	def specificReceive: Receive

	override final def receive = specificReceive.orElse[Any, Unit]{

		case Subscribe =>
			val publisher = sender()
			subscribers.add(publisher)
			context watch publisher
			publisher ! getStreamElement

		case Terminated(publisher) =>
			subscribers.remove(publisher)

	}.andThen{
		_ => notifySubscribers()
	}

}
