package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.Terminated
import akka.pattern.gracefulStop
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.stream.scaladsl.Flow
import akka.actor.Cancellable
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Keep

class StiltClusterApi {

	private val conf = ConfigLoader.load(Some("stiltfrontend.conf"))

	private val system = ActorSystem(conf.getString("stiltcluster.name"), conf)

	private val receptionist = system.actorOf(Props[WorkReceptionist], name = "receptionist")

	def addJob(job: Job): Unit = receptionist ! job

	def cancelJob(id: String): Unit = receptionist ! CancelJob(id)

	def dashboardInfo: Future[DashboardInfo] = {
		import system.dispatcher
		implicit val timeout: Timeout = (2 seconds)
		(receptionist ask GetStatus).map(_.asInstanceOf[DashboardInfo])
	}

	def shutdown()(implicit ctxt: ExecutionContext) = terminate(PoisonPill)
	def shutdownHard()(implicit ctxt: ExecutionContext) = terminate(StopAllWork)

	private def terminate(msg: Any)(implicit ctxt: ExecutionContext): Future[Terminated] = {
		gracefulStop(receptionist, 3 seconds, msg)
			.recover{case _ => false}
			.flatMap(_ => system.terminate())
	}

	val websocketsFlow: Flow[Any, DashboardInfo, Cancellable] = {
		val sink = Sink.ignore
		val source = Source.tick[Unit](0 seconds, 2 seconds, ())
			.mapAsync(1)(_ => dashboardInfo)
			.scan[(Option[DashboardInfo], DashboardInfo)]((None, null)){case ((emit, previous), next) =>
				if(next == previous) (None, previous)
				else (Some(next), next)
			}.collect{
				case (Some(di), _) => Future.successful(di)
			}
		Flow.fromSinkAndSourceMat(sink, source)(Keep.right)
			.keepAlive(30 seconds, () => dashboardInfo)
			.mapAsync(1)(di => di)
	}
}
