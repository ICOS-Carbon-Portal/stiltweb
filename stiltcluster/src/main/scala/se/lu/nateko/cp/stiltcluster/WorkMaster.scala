package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorRef
import java.nio.file.{Files, Paths}

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorSelection, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}


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


class WorkMaster(nCores: Int) extends Actor with Trace with Tracker {

	private var freeCores = nCores
	private var slotProd  = context.actorSelection("")

	traceSetPath(Paths.get("workmaster.log"))
	trace("WorkMaster starting up")

	def receive = slotCalculation orElse trackPeer

	def slotCalculation: Receive = {
		case CalculateSlot(slot: StiltSlot) =>
			if (freeCores <= 0) {
				trace("Received CalculateSlot even though I'm busy")
				sender() ! myStatus
			} else {
				startStilt(slot)
			}
	}

	def newPeerFound(sp: ActorSelection) = {
		trace(s"New slotproducer detected, sending greeting (${freeCores} free cores)")
		slotProd = sp
		slotProd ! myStatus
	}

	private def myStatus = WorkMasterStatus(freeCores)

	private def startStilt(slot: StiltSlot) = {
		import scala.concurrent.ExecutionContext.Implicits.global
		freeCores -= 1
		Future {
			trace(s"Starting stilt calculation of $slot")
			val s = RunStilt.cmd_run(slot)
			trace(s"Stilt simulation finished ${s}")
			val d = Paths.get(s)
			assert(Files.isDirectory(d))
			val r = StiltResult(slot, d.resolve("output"))
			slotProd ! SlotCalculated(r)
			freeCores += 1
			slotProd ! myStatus
		} onComplete {
			case Failure(t) => { trace("An error has occured: " + t.getStackTraceString) }
			case Success(_) => { }
		}
	}
}
