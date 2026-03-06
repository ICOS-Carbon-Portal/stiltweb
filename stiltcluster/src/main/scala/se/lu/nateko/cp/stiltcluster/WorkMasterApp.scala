package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.gracefulStop

object WorkMasterApp {

	def main(args: Array[String]): Unit = {
		val conf = ConfigLoader.workerNode()
		val system = ActorSystem("WorkMaster", conf)

		val coresForStilt: Int = {

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
			}(using global)
			Await.ready(done, 3.seconds)
		}
	}
}
