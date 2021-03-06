package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Try

import akka.actor.ActorSystem
import akka.pattern.gracefulStop

object WorkMasterApp extends App {

	val conf = ConfigLoader.workerNode()
	val system = ActorSystem("WorkMaster", conf)

	private val coresForStilt: Int = {

		val maxAllowedTry = Try{
			val maxVal = conf.getInt("stiltcluster.maxCores")
			if(maxVal <= 0) Int.MaxValue
			else maxVal
		}

		Math.min(
			maxAllowedTry.getOrElse(Int.MaxValue),
			Runtime.getRuntime.availableProcessors
		)
	}

	val wm = system.actorOf(
		WorkMaster.props(coresForStilt, conf.getString("stiltcluster.receptionistAddress")),
		name = "workmaster"
	)

	sys.addShutdownHook{
		val done = gracefulStop(wm, 2.seconds, WorkMaster.Stop).transformWith{
			_ => system.terminate()
		}(scala.concurrent.ExecutionContext.Implicits.global)
		Await.ready(done, 3.seconds)
	}

}
