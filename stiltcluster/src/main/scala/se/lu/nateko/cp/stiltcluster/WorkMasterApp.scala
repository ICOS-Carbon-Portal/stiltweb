package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.pattern.gracefulStop
import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object WorkMasterApp extends App {

	val conf = ConfigLoader.workerNode()
	val clusterConf = conf.getConfig("stiltcluster")

	val system = ActorSystem(clusterConf.getString("name"), conf)

	private val coresForStilt: Int = {

		val maxAllowedTry = Try{
			val maxVal = clusterConf.getInt("maxCores")
			if(maxVal <= 0) Int.MaxValue
			else maxVal
		}

		Math.min(
			maxAllowedTry.getOrElse(Int.MaxValue),
			2 * Runtime.getRuntime.availableProcessors
		)
	}

	val wm = system.actorOf(WorkMaster.props(coresForStilt), name = "workmaster")

	sys.addShutdownHook{
		val done = gracefulStop(wm, 2.seconds, WorkMaster.Stop).transformWith{
			_ => system.terminate()
		}(scala.concurrent.ExecutionContext.Implicits.global)
		Await.ready(done, 3.seconds)
	}

}
