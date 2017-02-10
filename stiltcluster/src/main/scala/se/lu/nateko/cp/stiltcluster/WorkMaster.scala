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
	}

	def receive = {

		case job: Job => if(freeCores > 0) {
			val run = JobRun(job, preferredParallelism)
			val worker = context.actorOf(Worker.props(conf))
			val id = run.runId
			workers += ((id, worker))
			runs += ((id, run))
			status += ((id, JobStatus.init(id)))
			worker ! run
			sender() ! run
		}

		case jc: CancelJob =>
			workers.get(jc.id).foreach(_ ! jc)

		case GetStatus => sender ! myStatus

		case Thanks(ids) =>
			ids.filter(id => !workers.contains(id)).foreach{id =>
				runs -= id
				status -= id
			}

		case js: JobStatus =>
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

		case StopAllWork =>
			if(workers.isEmpty) context stop self
			else {
				workers.foreach{
					case (id, worker) => worker ! CancelJob(id)
				}
				context become shuttingDown
			}

	}

	def shuttingDown: Receive = {
		case JobCanceled(js) =>
			sender() ! PoisonPill
			workers -= js.id
			if(workers.isEmpty) context stop self
	}

	private def myStatus = WorkMasterStatus(
		work = runs.keys.map{id => (runs(id), status(id))}.toSeq,
		freeCores = freeCores
	)

	private def freeCores: Int = {
		val occupied = runs.values.collect{
			case run if status.get(run.runId).flatMap(_.exitValue).isEmpty => run.parallelism
		}.sum
		Math.max(coresPoolSize - occupied, 0)
	}

	private def preferredParallelism: Int = Math.ceil(freeCores.toDouble / 2).toInt

	private def register(member: Member): Unit = if (member.hasRole("frontend")) {
		context.actorSelection(
			RootActorPath(member.address) / "user" / "receptionist"
		) ! WorkMasterRegistration(coresPoolSize)
	}
}

object WorkMaster{

	def props(conf: StiltEnv) = Props(classOf[WorkMaster], conf, 0)
	def props(conf: StiltEnv, reservedCores: Int) = Props(classOf[WorkMaster], conf, reservedCores)

}
