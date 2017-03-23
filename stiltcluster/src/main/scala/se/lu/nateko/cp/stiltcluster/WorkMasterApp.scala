package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.pattern.gracefulStop

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

object WorkMasterApp extends App {

	val conf = ConfigLoader.load()

	val thisIsFrontendVm: Boolean = {
		val hostname = conf.getString("akka.remote.netty.tcp.hostname")
		val seedHostname = conf.getString("stiltcluster.seedhost")
		hostname == seedHostname
	}

	val system = ActorSystem(conf.getString("stiltcluster.name"), conf)

	val reservedCores = if(thisIsFrontendVm) 2 else 0

	val workMasterProps = WorkMaster.props(ConfigLoader.loadStiltEnv, reservedCores)
	val worker = system.actorOf(workMasterProps, name = "backend")

	sys.addShutdownHook{
		if(!system.whenTerminated.isCompleted){
			import scala.concurrent.ExecutionContext.Implicits.global
			val done = gracefulStop(worker, 3 seconds, StopAllWork)
				.recover{case _ => false}
				.flatMap(_ => system.terminate())
			Await.result(done, 5 seconds)
		}
	}
}
