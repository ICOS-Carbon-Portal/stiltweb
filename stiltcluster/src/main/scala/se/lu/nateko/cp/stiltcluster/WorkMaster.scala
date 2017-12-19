package se.lu.nateko.cp.stiltcluster

import java.nio.file.Paths

import akka.actor.{ Actor, ActorSelection, RootActorPath }
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
import akka.cluster.{ Cluster, Member, MemberStatus }
import akka.cluster.ClusterEvent.{ CurrentClusterState, MemberUp }
import akka.actor.Props


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
	protected val traceFile = Paths.get("workmaster.log")

	trace("WorkMaster starting up")

	override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 1, loggingEnabled = true){
		SupervisorStrategy.defaultDecider
	}

	def receive = slotCalculation orElse trackPeer

	def slotCalculation: Receive = {
		case CalculateSlot(slot: StiltSlot) =>
			if (freeCores <= 0)
				trace("Received CalculateSlot even though I'm busy")
			else {
				freeCores -= 1
				context.actorOf(Worker.props(slot))
			}
			sender() ! myStatus

		case result: SlotCalculated =>
			finishSlot(result)
		case failure: StiltFailure =>
			finishSlot(failure)

		case WorkMaster.Stop =>
			trace(s"Terminated (was $self)")
			context stop self
	}

	private def finishSlot(msg: Any): Unit = {
		slotProd ! msg
		freeCores += 1
		slotProd ! myStatus
	}

	def newPeerFound(sp: ActorSelection) = {
		trace(s"New slotproducer detected, sending greeting (${freeCores} free cores)")
		slotProd = sp
		slotProd ! myStatus
	}

	private def myStatus = WorkMasterStatus(freeCores, nCores)
}

object WorkMaster{
	def props(nCores: Int) = Props.create(classOf[WorkMaster], Int.box(nCores))

	case object Stop
}
