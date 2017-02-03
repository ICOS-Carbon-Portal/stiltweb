package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.RootActorPath
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.CurrentClusterState

class WorkMaster extends Actor{

	val cluster = Cluster(context.system)

	override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
	override def postStop(): Unit = {
		cluster.leave(cluster.selfAddress)
		cluster.unsubscribe(self)
	}

	def receive = {

		case state: CurrentClusterState =>
			state.members.filter(_.status == MemberStatus.Up) foreach register

		case MemberUp(m) => register(m)
	}

	def register(member: Member): Unit = if (member.hasRole("frontend")) {
		context.actorSelection(
			RootActorPath(member.address) / "user" / "frontend"
		) ! WorkMasterRegistration
	}
}
