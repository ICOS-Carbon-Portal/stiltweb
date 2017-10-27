package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Future

import akka.actor.{Actor, ActorLogging, ActorSelection, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import se.lu.nateko.cp.stiltrun.RunStilt


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


class WorkMaster(nCores: Int) extends Actor with ActorLogging with Tracker {

	private var freeCores = nCores

	def receive = slotCalculation orElse trackPeer

	log.info("WorkMaster starting up")

	def slotCalculation: Receive = {
		case CalculateSlot(slot: StiltSlot) =>
			log.info("Received CalculateSlot")
			if (freeCores <= 0) {
				sender() ! myStatus
			} else {
				log.info("Starting Stilt")
				startStilt(slot)
			}
	}

	def newPeerFound(sp: ActorSelection) = {
		log.info("New slotproducer detected, sending greeting")
		sp ! myStatus
	}

	private def myStatus = WorkMasterStatus(freeCores)

	private def startStilt(slot: StiltSlot) = {
		import scala.concurrent.ExecutionContext.Implicits.global
		freeCores -= 1
		val orgSender = sender()
		Future {
			log.info(s"Starting stilt calculation of $slot")
			val _ = RunStilt.cmd_run(slot)
//			val r = new StiltSlotResultMap(slot)
			log.info(s"Stilt simulation finished")
			orgSender ! SlotCalculated(slot)
			orgSender ! myStatus
			freeCores += 1
		}
	}

	// private def zipDir(job: Job, slot: String, dir: String) = {
	//	val tmp = File.createTempFile("slot", "zip")
	//	val cmd = s"zip -r --quiet ${tmp.getPath} ${dir}"

	//	import scala.sys.process._
	//	import scala.concurrent.ExecutionContext.Implicits.global
	//	val orgSender = sender()

	//	Future {
	//		log.info(s"Executing ${cmd}")
	//		cmd.!
	//		val blob = Files.readAllBytes(Paths.get(tmp.toString))
	//		log.info(s"Read ${blob.length} bytes of zip data")
	//		orgSender !
	//	}
	// }
}
