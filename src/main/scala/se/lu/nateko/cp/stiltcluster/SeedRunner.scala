package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.Props

class SeedRunner {

	val conf = ConfigLoader.load(Some("stiltseed.conf"))

	val system = ActorSystem("StiltCluster", conf)

	system.actorOf(Props[WorkReceptionist], name = "frontend")
	val workMasterProps = WorkMaster.props(ConfigLoader.loadStiltEnv, 2)
	system.actorOf(workMasterProps, name = "backend")
}
