package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Future

import akka.actor.{Actor, ActorLogging, ActorSelection, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import se.lu.nateko.cp.stiltrun.RunStilt


trait ReceptionistTracker extends Actor {

	val cluster = Cluster(context.system)

	override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
	override def postStop(): Unit = cluster.unsubscribe(self)

	def trackReceptionist: Receive = {
		case state: CurrentClusterState =>
			state.members.filter(_.status == MemberStatus.Up) foreach register
		case MemberUp(m) => register(m)
	}

	def register(member: Member): Unit =
		if (member.hasRole("frontend")) {
			newReceptionist(context.actorSelection(
				RootActorPath(member.address) / "user" / "receptionist"))
		}

	def newReceptionist(receptionist: ActorSelection): Unit
}



class WorkMaster(nCores: Int) extends Actor with ActorLogging with ReceptionistTracker {

	private var freeCores = nCores

	def receive = slotCalculation orElse trackReceptionist

	def slotCalculation: Receive = {
		case CalculateSlot(job: Job, slot: String) =>
			if (freeCores <= 0) {
				sender() ! myStatus
			} else {
				startStilt(job, slot)
			}
	}


	def newReceptionist(r: ActorSelection) = r ! myStatus

	private def myStatus = WorkMasterStatus(freeCores)

	private def startStilt(job: Job, slot: String) = {
		import scala.concurrent.ExecutionContext.Implicits.global
		freeCores -= 1
		val orgSender = sender()
		Future {
			log.info(s"Starting stilt calculation of $job / $slot")
			val _ = RunStilt.cmd_run(job, slot)
			orgSender ! SlotCalculated(job, slot)
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
