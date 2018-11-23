package se.lu.nateko.cp.stiltcluster

import java.nio.file.Paths

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.pattern.ask
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.util.Timeout
import se.lu.nateko.cp.stiltweb.ConfigReader
import se.lu.nateko.cp.stiltweb.state.Archiver


class StiltClusterApi {

	private val conf = ConfigLoader.frontNode()

	private val system = ActorSystem("StiltBoss", conf)

	val stiltConf = ConfigReader.default

	val stateDir = {
		val dirPath = stiltConf.stateDirectory
		Paths.get(dirPath.replaceFirst("^~", System.getProperty("user.home")))
	}

	val archiver = new Archiver(stateDir, stiltConf.slotStepInMinutes)

	val receptionist = ActorSelection(
		system.actorOf(WorkReceptionist.props(archiver), name = "receptionist"),
		Iterable.empty
	)


	import system.dispatcher

	def enqueueJob(job: Job): Unit = receptionist ! job

	def cancelJob(id: String): Unit = receptionist ! CancelJob(id)

	def queryOwner(jobId: String): Future[Option[String]] = {
		// The assumption is that this query will run on the same JVM as the responding actor.
		implicit val timeout = Timeout(1 seconds)
		ask(receptionist, PleaseSendDashboardInfo).mapTo[DashboardInfo].map{ dbi =>
			dbi.findCancellableJobById(jobId).map(_.userId)
		}
	}

	val websocketsFlow: Flow[Message, Message, Any] = {
		import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
		import spray.json._
		import akka.http.scaladsl.model.ws.TextMessage.Strict

		val source: Source[Message, Any] = Source
			.actorRef[DashboardInfo](1, OverflowStrategy.dropHead)
			.mapMaterializedValue(publisher => receptionist.tell(Subscribe, publisher))
			.map(di => Strict(di.toJson.compactPrint))

		Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
			.keepAlive(30 seconds, () => Strict(""))
	}

	def terminate() = system.terminate()
}
