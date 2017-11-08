package se.lu.nateko.cp.stiltcluster

import akka.actor.{ActorSystem, Props}

object WorkMasterApp extends App {

	val conf = ConfigLoader.load()
	val system = ActorSystem(conf.getString("stiltcluster.name"), conf)
	val worker = system.actorOf(Props(new WorkMaster(coresForStilt)),
								name = "workmaster")

	private def coresForStilt: Int = {
		val thisIsFrontendVm: Boolean = {
			val hostname = conf.getString("akka.remote.netty.tcp.hostname")
			val seedHostname = conf.getString("stiltcluster.seedhost")
			hostname == seedHostname
		}
		val reservedCores = if(thisIsFrontendVm) 2 else 0
		Runtime.getRuntime.availableProcessors - reservedCores
	}

}
