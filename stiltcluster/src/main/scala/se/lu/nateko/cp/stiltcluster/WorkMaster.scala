package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.Map

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.Terminated
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Member
import akka.cluster.MemberStatus

class WorkMaster(conf: StiltEnv, reservedCores: Int) extends Actor{

	private val log = context.system.log
	val cluster = Cluster(context.system)
	val coresPoolSize = Runtime.getRuntime.availableProcessors - reservedCores

	val workers = Map.empty[String, ActorRef]
	val runs = Map.empty[String, JobRun]
	val status = Map.empty[String, ExecutionStatus]

	private val devnull = context.system.actorOf(Props.empty)

	private var receptionist: ActorRef = devnull

	override def preStart(): Unit = {
		cluster.subscribe(self, classOf[MemberUp])
	}

	override def postStop(): Unit = {
		cluster.unsubscribe(self)
		cluster.leave(cluster.selfAddress)
	}

	def receive = {

		case Hi =>
			receptionist = sender()
			context watch receptionist

		case Terminated(dead) =>
			if(receptionist == dead) receptionist = devnull

		case job: Job => if(freeCores == 0) {
			receptionist ! myStatus
			receptionist ! job
		} else {
			val run = JobRun(job, preferredParallelism)
			val worker = context.actorOf(Worker.props(conf, self))
			val id = run.job.id
			workers += ((id, worker))
			runs += ((id, run))
			status += ((id, ExecutionStatus.init(id)))
			worker ! run
			receptionist ! myStatus
		}

		case jc: CancelJob =>
			log.info(s"Workmaster passsing on CancelJob request ${jc.id}")
			workers.get(jc.id).foreach(_ ! jc)

		case Thanks(ids) =>
			ids.filter(id => !workers.contains(id)).foreach{id =>
				runs -= id
				status -= id
			}

		case es: ExecutionStatus =>
			status += ((es.id, es))
			if(es.exitValue.isDefined){
				sender() ! PoisonPill
				workers -= es.id
			}
			receptionist ! myStatus

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

		case JobCanceled(id) =>
			workers -= id
			runs -= id
			status -= id
			sender() ! PoisonPill
			receptionist ! myStatus
		case unknown =>
			log.info(s"Workmaster received unknown messager ${unknown}")

	}

	def shuttingDown: Receive = {
		case JobCanceled(id) =>
			sender() ! PoisonPill
			workers -= id
			if(workers.isEmpty) context stop self
	}

	private def myStatus = WorkMasterStatus(
		work = runs.keys.map{id => (runs(id), status(id))}.toVector,
		freeCores = freeCores
	)

	private def freeCores: Int = {
		val occupied = runs.values.collect{
			case run if status.get(run.job.id).flatMap(_.exitValue).isEmpty => run.parallelism
		}.sum
		Math.max(coresPoolSize - occupied, 0)
	}

	private def preferredParallelism: Int = Math.ceil(freeCores.toDouble / 2).toInt

	private def register(member: Member): Unit = if (member.hasRole("frontend")) {
		context.actorSelection(
			RootActorPath(member.address) / "user" / "receptionist"
		) ! myStatus
	}
}

object WorkMaster{

	def props(conf: StiltEnv) = Props(classOf[WorkMaster], conf, 0)
	def props(conf: StiltEnv, reservedCores: Int) = Props(classOf[WorkMaster], conf, reservedCores)

}
