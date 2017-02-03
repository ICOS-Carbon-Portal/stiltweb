package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import java.io.File

object WorkMasterApp extends App {

	println("Current dir: " + new File(".").getAbsolutePath)

	val conf = ConfigFactory.parseFile(new File("application.conf"))
		.withFallback(ConfigFactory.parseResources("stiltcluster.conf"))
		.resolve()
		.withFallback(ConfigFactory.load())

	val system = ActorSystem("StiltCluster", conf)
	val worker = system.actorOf(Props[WorkMaster], name = "backend")

}
