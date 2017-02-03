package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.ConfigFactory
import java.io.File
import akka.actor.ActorSystem
import akka.actor.Props

class SeedRunner {

	val conf = ConfigFactory.parseFile(new File("application.conf"))
		.withFallback(ConfigFactory.parseResources("stiltseed.conf"))
		.withFallback(ConfigFactory.parseResources("stiltcluster.conf"))
		.resolve()
		.withFallback(ConfigFactory.load())

	val system = ActorSystem("StiltCluster", conf)

	system.actorOf(Props[WorkReceptionist], name = "frontend")
}