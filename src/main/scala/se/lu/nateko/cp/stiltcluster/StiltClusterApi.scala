package se.lu.nateko.cp.stiltcluster

import java.nio.file.Paths

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.ws.Message
import akka.pattern.ask
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.util.Timeout
import se.lu.nateko.cp.stiltweb.ConfigReader


class StiltClusterApi {

	private val conf = ConfigLoader.frontNode()

	private val system = ActorSystem("StiltCluster", conf)

	val stiltConf = ConfigReader.default

	val stateDir = {
		val dirPath = stiltConf.stateDirectory
		Paths.get(dirPath.replaceFirst("^~", System.getProperty("user.home")))
	}

	val mainDir = Paths.get(stiltConf.mainDirectory)
	val slotStep = stiltConf.slotStepInMinutes

	val dashboard = system.actorOf(Props[DashboardMaker], name="dashboardmaker")

	private val archiver =  new SlotArchiver(stateDir, slotStep)
	system.actorOf(SlotProducer.props(archiver), name="slotproducer")

	val receptionist = system.actorOf(WorkReceptionist.props(stateDir, slotStep), name = "receptionist")



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
		import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
		import spray.json._
		import akka.http.scaladsl.model.ws.TextMessage.Strict

		val source: Source[Message, Any] = Source
			.actorRef[DashboardInfo](1, OverflowStrategy.dropHead)
			.mapMaterializedValue(publisher => dashboard.tell(Subscribe, publisher))
			.map(di => Strict(di.toJson.compactPrint))

		Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
			.keepAlive(30 seconds, () => Strict(""))
	}
}
