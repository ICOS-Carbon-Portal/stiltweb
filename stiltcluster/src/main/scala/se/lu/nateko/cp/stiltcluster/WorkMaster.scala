package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.RootActorPath
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.actor.Props
import java.time.temporal.ChronoUnit
import akka.actor.ActorRef
import scala.collection.mutable.Map
import scala.concurrent.duration.DurationInt
import akka.actor.PoisonPill

class WorkMaster(conf: StiltEnv, reservedCores: Int) extends Actor{

	val cluster = Cluster(context.system)
	val coresPoolSize = Runtime.getRuntime.availableProcessors - reservedCores

	val workers = Map.empty[String, ActorRef]
	val runs = Map.empty[String, JobRun]
	val status = Map.empty[String, JobStatus]

	override def preStart(): Unit = {
		cluster.subscribe(self, classOf[MemberUp])
		context.system.scheduler.schedule(2 seconds, 2 seconds, self, CollectStatus)(context.system.dispatcher)
	}

	override def postStop(): Unit = {
		cluster.unsubscribe(self)
		cluster.leave(cluster.selfAddress)
		context.system.terminate()
	}

	def receive = {

		case job: Job => if(freeCores > 0) {
			val run = JobRun(job, preferredParallelism)
			val worker = context.actorOf(Worker.props(conf))
			workers += ((run.runId, worker))
			worker ! run
			sender() ! run
		}

		case GetStatus => sender ! myStatus

		case Thanks(ids) =>
			ids.filter(id => !workers.contains(id)).foreach{id =>
				runs -= id
				status -= id
			}

		case js: JobStatus =>
			context.system.log.info(js.toString)
			status += ((js.id, js))
			if(js.exitValue.isDefined){
				sender() ! PoisonPill
				workers -= js.id
			}

		case CollectStatus =>
			workers.values.foreach(_ ! GetStatus)

		case state: CurrentClusterState =>
			state.members.filter(_.status == MemberStatus.Up) foreach register

		case MemberUp(m) => register(m)

		case StopWorkMaster =>
			val boss = sender()

			if(workers.isEmpty) boss ! myStatus
			else workers.foreach{
				case (id, worker) => worker ! CancelJob(id)
			}
			context become shuttingDown(boss)

	}

	def shuttingDown(boss: ActorRef): Receive = {
		case js: JobStatus =>
			status += ((js.id, js))
			sender() ! PoisonPill
			workers -= js.id

			if(workers.isEmpty) boss ! myStatus

		case finalThanks: Thanks =>
			context stop self
	}

	private def myStatus = WorkMasterStatus(
		work = runs.keys.map{id => (runs(id), status(id))}.toSeq,
		freeCores = freeCores
	)

	private def freeCores: Int = {
		val occupied = runs.values.map(_.parallelism).sum
		Math.max(coresPoolSize - occupied, 0)
	}

	private def preferredParallelism: Int = Math.ceil(freeCores.toDouble / 2).toInt

	private def register(member: Member): Unit = if (member.hasRole("frontend")) {
		context.actorSelection(
			RootActorPath(member.address) / "user" / "frontend"
		) ! WorkMasterRegistration
	}
}

object WorkMaster{

	def props(conf: StiltEnv) = Props(classOf[WorkMaster], conf, 0)
	def props(conf: StiltEnv, reservedCores: Int) = Props(classOf[WorkMaster], conf, reservedCores)

}
