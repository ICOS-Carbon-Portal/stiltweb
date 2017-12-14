package se.lu.nateko.cp.stiltcluster

import akka.actor.{ActorSystem, Props}

object WorkMasterApp extends App {

	val conf = ConfigLoader.clusterBase()
	val system = ActorSystem(conf.getString("stiltcluster.name"), conf)
	val worker = system.actorOf(Props(new WorkMaster(coresForStilt)),
								name = "workmaster")

	private def coresForStilt: Int = {
		val thisIsFrontendVm: Boolean = {
			false
		}
		val reservedCores = if(thisIsFrontendVm) 1 else 0
		Runtime.getRuntime.availableProcessors - reservedCores
	}

}
