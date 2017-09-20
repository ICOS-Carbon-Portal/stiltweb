package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.Terminated
import akka.pattern.gracefulStop
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Keep
import akka.http.scaladsl.model.ws.Message
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

import se.lu.nateko.cp.stiltweb.StiltJsonSupport

class StiltClusterApi {

	private val conf = ConfigLoader.load(Some("stiltfrontend.conf"))

	private val system = ActorSystem(conf.getString("stiltcluster.name"), conf)
	import system.dispatcher
	private val receptionist = system.actorOf(Props[WorkReceptionist], name = "receptionist")

	def addJob(job: Job): Unit = receptionist ! job

	def cancelJob(id: String): Unit = receptionist ! CancelJob(id)

	def queryOwner(jobId: String): Future[Option[String]] = {
		// The assumption is that this query will run on the same JVM as the responding actor.
		implicit val timeout = Timeout(1 seconds)
		ask(receptionist, PleaseSendDashboardInfo).mapTo[DashboardInfo].map{ dbi =>
			dbi.findCancellableJobById(jobId).map(_.userId)
		}
	}

	def shutdown()() = terminate(PoisonPill)
	def shutdownHard()() = terminate(StopAllWork)

	private def terminate(msg: Any): Future[Terminated] = {
		gracefulStop(receptionist, 3 seconds, msg)
			.recover{case _ => false}
			.flatMap(_ => system.terminate())
	}

	val websocketsFlow: Flow[Message, Message, Any] = {
		import StiltJsonSupport._
		import spray.json._
		import akka.http.scaladsl.model.ws.TextMessage.Strict

		val source: Source[Message, Any] = Source
			.actorPublisher[DashboardInfo](DashboardPublisher.props(receptionist))
			.map(di => Strict(di.toJson.compactPrint))

		Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
			.keepAlive(30 seconds, () => Strict(""))
	}
}
