package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.gracefulStop

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

object WorkMasterApp extends App {

	val conf = ConfigLoader.load()

	val system = ActorSystem("StiltCluster", conf)

	val workMasterProps = WorkMaster.props(ConfigLoader.loadStiltEnv)
	val worker = system.actorOf(workMasterProps, name = "backend")

	sys.addShutdownHook{
		if(!system.whenTerminated.isCompleted){
			import scala.concurrent.ExecutionContext.Implicits.global
			val done = gracefulStop(worker, 5 seconds, StopWorkMaster)
				.flatMap(_ => system.whenTerminated)
			Await.result(done, 6 seconds)
		}
	}
}
