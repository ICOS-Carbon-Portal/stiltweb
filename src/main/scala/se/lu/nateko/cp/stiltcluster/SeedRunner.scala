package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.Props

class SeedRunner {

	//TODO Add proper termination support

	val conf = ConfigLoader.load(Some("stiltfrontend.conf"))

	val system = ActorSystem("StiltCluster", conf)

	system.actorOf(Props[WorkReceptionist], name = "receptionist")
}
