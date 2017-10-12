package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future


class StiltClusterApi {

	private val conf = ConfigLoader.load(Some("stiltfrontend.conf"))
	private val system = ActorSystem(conf.getString("stiltcluster.name"), conf)

	private val receptionist = system.actorOf(
		Props[WorkReceptionist], name = "receptionist")

	val archiveDir = ConfigLoader.loadStiltEnv.archiveDirectory
	system.actorOf(Props[SlotCalculator], name="slotcalculator")
	system.actorOf(Props(new JobArchiver(archiveDir)), name="jobarchiver")
	system.actorOf(Props(new SlotArchiver(archiveDir)), name="slotarchiver")
	system.actorOf(Props[SlotProducer], name="slotproducer")

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

	// val websocketsFlow: Flow[Message, Message, Any] = {
	//	import StiltJsonSupport._
	//	import spray.json._
	//	import akka.http.scaladsl.model.ws.TextMessage.Strict

	//	val source: Source[Message, Any] = Source
	//		.actorPublisher[DashboardInfo](DashboardPublisher.props(receptionist))
	//		.map(di => Strict(di.toJson.compactPrint))

	//	Flow.fromSinkAndSourceMat(Sink.ignore, source)(Keep.right)
	//		.keepAlive(30 seconds, () => Strict(""))
	// }
}
