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
import akka.http.scaladsl.model.ws.Message
import se.lu.nateko.cp.stiltweb.StiltJsonSupport

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

	val websocketsFlow: Flow[Message, Message, Cancellable] = {
		import StiltJsonSupport._
		import spray.json._
		import akka.http.scaladsl.model.ws.TextMessage.Strict

		val source: Source[Message, Cancellable] = Source
			.tick[Unit](0 seconds, 2 seconds, ())
			.mapAsync(1)(_ => dashboardInfo)
			.scan[(Option[Message], DashboardInfo)]((None, null)){
				case ((emit, previous), next) =>
					if(next == previous)
						(None, previous)
					else
						(Some(Strict(next.toJson.compactPrint)), next)
			}.collect{
				case (Some(msg), _) => msg
			}
		Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
			.keepAlive(30 seconds, () => Strict(""))
	}
}
