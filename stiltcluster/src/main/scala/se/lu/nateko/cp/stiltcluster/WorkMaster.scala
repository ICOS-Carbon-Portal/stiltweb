package se.lu.nateko.cp.stiltcluster

import akka.actor.{ Actor, ActorSelection, RootActorPath }
import akka.actor.Cancellable
import akka.actor.Props
import akka.cluster.{ Cluster, Member, MemberStatus }
import akka.cluster.ClusterEvent.{ CurrentClusterState, MemberUp }

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import java.nio.file.Files
import java.nio.file.Paths


trait Tracker extends Actor {

	val cluster = Cluster(context.system)

	override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
	override def postStop(): Unit = cluster.unsubscribe(self)

	def trackPeer: Receive = {
		case state: CurrentClusterState =>
			state.members.filter(_.status == MemberStatus.Up) foreach register
		case MemberUp(m) => register(m)
	}

	def register(member: Member): Unit =
		if (member.hasRole("frontend")) {
			newPeerFound(context.actorSelection(
				RootActorPath(member.address) / "user" / "slotproducer"))
		}

	def newPeerFound(as: ActorSelection): Unit
}


class WorkMaster(nCores: Int) extends Trace with Tracker {

	private var freeCores = nCores
	private var slotProd  = context.actorSelection("")
	private var tickCancel: Cancellable = null
	protected val traceFile = Paths.get("workmaster.log")

	override def preStart(): Unit = {
		super.preStart()
		trace("WorkMaster starting up")
		implicit val ctxt = context.system.dispatcher
		tickCancel = context.system.scheduler.schedule(30.seconds, 30.seconds, self, WorkMaster.Tick)
	}

	override def postStop(): Unit = {
		super.postStop()
		if(tickCancel != null) tickCancel.cancel()
	}

	def receive = slotCalculation orElse trackPeer

	def slotCalculation: Receive = {
		case CalculateSlot(slot: StiltSlot) =>
			if (freeCores <= 0)
				trace("Received CalculateSlot even though I'm busy")
			else {
				freeCores -= 1
				calculateSlot(slot)
			}
			sender() ! myStatus

		case WorkMaster.Tick =>
			slotProd ! myStatus

		case WorkMaster.Stop =>
			trace(s"Terminated (was $self)")
			context stop self
	}

	private def finishSlot(msg: Any): Unit = {
		freeCores += 1
		slotProd ! msg
		slotProd ! myStatus
	}

	def newPeerFound(sp: ActorSelection) = {
		trace(s"New slotproducer detected, sending greeting (${freeCores} free cores)")
		slotProd = sp
		slotProd ! myStatus
	}

	private def myStatus = WorkMasterStatus(freeCores, nCores)

	private def calculateSlot(slot: StiltSlot): Unit = {

		import scala.concurrent.ExecutionContext.Implicits.global

		def calculateOnce(): Future[Unit] = Future{
			trace(s"Starting stilt calculation of $slot")

			val stiltOutput = RunStilt.cmd_run(slot)

			trace(s"Stilt simulation finished $slot ($stiltOutput)")

			val d = Paths.get(stiltOutput)
			assert(Files.isDirectory(d))

			finishSlot(SlotCalculated(StiltResult(slot, d.resolve("output"))))

			trace(s"Slot calculation for $slot was a success")
		}

		calculateOnce().recoverWith{ case _: Throwable => calculateOnce()}
			.failed
			.foreach{err =>
				trace(s"Slot calculation for $slot was a failure: " + err.getMessage)
				trace(err.getStackTrace().mkString("", "\n", "\n"))
				finishSlot(StiltFailure(slot))
			}
	}
}

object WorkMaster{
	def props(nCores: Int) = Props.create(classOf[WorkMaster], Int.box(nCores))

	case object Stop
	case object Tick
}
