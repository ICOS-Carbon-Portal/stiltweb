package se.lu.nateko.cp.stiltcluster

import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import se.lu.nateko.cp.stiltweb.ConfigReader
import java.nio.file.Paths


class StiltClusterApi {

	private val conf = ConfigLoader.frontNode()

	private val system = ActorSystem(conf.getString("stiltcluster.name"), conf)


	val stateDir = {
		val dirPath = ConfigReader.default.stateDirectory
		Paths.get(dirPath.replaceFirst("^~", System.getProperty("user.home")))
	}

	val mainDir = Paths.get(ConfigReader.default.mainDirectory)

	val receptionist = system.actorOf(WorkReceptionist.props(mainDir), name = "receptionist")

	system.actorOf(Props(new SlotArchiver(stateDir)), name="slotarchiver")
	system.actorOf(Props[SlotCalculator], name="slotcalculator")
	system.actorOf(Props(new SlotProducer(stateDir.resolve("slotproducer.log"))),
						 name="slotproducer")
	system.actorOf(Props(new JobArchiver(stateDir)), name="jobarchiver")
	val dashboard = system.actorOf(Props[DashboardMaker], name="dashboardmaker")

	import system.dispatcher

	def enqueueJob(job: Job): Unit = receptionist ! job

	def cancelJob(id: String): Unit = receptionist ! CancelJob(id)

	def queryOwner(jobId: String): Future[Option[String]] = {
		// The assumption is that this query will run on the same JVM as the responding actor.
		implicit val timeout = Timeout(1 seconds)
		ask(dashboard, PleaseSendDashboardInfo).mapTo[DashboardInfo].map{ dbi =>
			dbi.findCancellableJobById(jobId).map(_.userId)
		}
	}

	val websocketsFlow: Flow[Message, Message, Any] = {
		import se.lu.nateko.cp.stiltweb.StiltJsonSupport._
		import spray.json._
		import akka.http.scaladsl.model.ws.TextMessage.Strict

		val source: Source[Message, Any] = Source
			.actorPublisher[DashboardInfo](DashboardPublisher.props(dashboard))
			.map(di => Strict(di.toJson.compactPrint))

		Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
			.keepAlive(30 seconds, () => Strict(""))
	}
}
