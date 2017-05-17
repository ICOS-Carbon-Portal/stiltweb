package se.lu.nateko.cp.stiltcluster

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


/** A WorkMaster uses Workers to run Jobs on available CPUs.
  *
  * It receives Jobs from a WorkReceptionist, checks available CPUs and starts
  * new Workers. It then receives steady status updates from its Workers which
  * it passes on to the WorkReceptionist.
  *
  * There will typically be a single WorkMaster on each machine meant for
  * running simulations.
  */
class WorkMaster(conf: StiltEnv, reservedCores: Int) extends Actor{

	import WorkMaster.WorkInProgress
	private val running = scala.collection.mutable.Map.empty[String, WorkInProgress]

	private val log = context.system.log
	private val cluster = Cluster(context.system)

	private val devnull = context.system.actorOf(Props.empty)
	private var receptionist: ActorRef = devnull

	def receive = {

		case Hi =>
			receptionist = sender()
			context watch receptionist

		case Terminated(dead) =>
			if(receptionist == dead) receptionist = devnull

		// The WorkReceptionist is giving us a new job; it's supposed to have
		// checked whether we can actually accept it (i.e if we have free
		// cores), in case it was mistaken we send the job back
		case job: Job => if(freeCores == 0) {
			receptionist ! myStatus
			receptionist ! job
		} else {
			if (running.contains(job.id)) {
				log.error(s"WorkMaster already has a job running with id ${job.id}")
			} else {
				val wip = WorkInProgress(context.actorOf(Worker.props(conf, self)),
										 job, preferredParallelism,
										 ExecutionStatus.init(job.id))

				running(job.id) = wip
				wip.worker ! ((job, wip.parallelism))
				receptionist ! myStatus
			}
		}

		// The WorkReceptionist wants us to cancel a job, pass the request along
		// to the correct worker.
		case jc: CancelJob =>
			running.get(jc.id).foreach(_.worker ! jc)

		// A worker has sucessfully canceled a job
		case JobCanceled(id) =>
			running.remove(id)
			sender() ! PoisonPill
			receptionist ! myStatus

		// The WorkReceptionist has received our status update and wants us to
		// clear our knowledge of those jobs that are complete.
		case Thanks(ids) =>
			ids.foreach{ id =>
				running.remove(id) match {
					case None      =>
						log.warning(s"WorkMaster - cannot delete nonexisting job $id")
					case Some(wip) =>
						if(wip.status.exitValue.isEmpty)
							log.warning(s"WorkMaster - removing incomplete job $id")
				}
			}

		// One of our workers is updating us on its progress
		case s: ExecutionStatus =>
			// Look up our WorkInProgress - it must exist.
			val wip = running(s.id)
			// Update its status field with current status
			running(s.id) = wip.copy(status=s)
			// If the process has exited (i.e, the actual stilt unix process has
			// exited) then shut down the worker and inform the receptionist.
			if(s.exitValue.isDefined){
				sender() ! PoisonPill
			}
			receptionist ! myStatus

		case state: CurrentClusterState =>
			state.members.filter(_.status == MemberStatus.Up) foreach register

		case MemberUp(m) => register(m)

		case StopAllWork =>
			running.isEmpty match {
				case true  => context stop self
				case false => running.foreach{
					case (id, wip) => wip.worker ! CancelJob(id)
				}
				context become shuttingDown
			}

		case unknown =>
			log.info(s"Workmaster received unknown messager ${unknown}")

	}

	def shuttingDown: Receive = {
		case JobCanceled(id) =>
			sender() ! PoisonPill
			running -= id
			if(running.isEmpty) context stop self
	}

	// Helpers
	private def myStatus: WorkMasterStatus =
		WorkMasterStatus(running.values.map { wip => (wip.job, wip.status) }.toList, freeCores)

	private def freeCores: Int = {
		val max  = Runtime.getRuntime.availableProcessors - reservedCores
		val used = running.values.collect{case w if ! w.status.exitValue.isDefined => w.parallelism }.sum
		Math.max(max - used, 0)
	}

	private def preferredParallelism: Int = Math.ceil(freeCores.toDouble / 2).toInt


	// AKKA Stuff
	override def preStart(): Unit = {
		cluster.subscribe(self, classOf[MemberUp])
	}

	override def postStop(): Unit = {
		cluster.unsubscribe(self)
		cluster.leave(cluster.selfAddress)
	}

	private def register(member: Member): Unit = if (member.hasRole("frontend")) {
		context.actorSelection(
			RootActorPath(member.address) / "user" / "receptionist"
		) ! myStatus
	}
}

object WorkMaster{

	private case class WorkInProgress(worker: ActorRef, job: Job, parallelism: Int, status: ExecutionStatus)

	def props(conf: StiltEnv) = Props(classOf[WorkMaster], conf, 0)
	def props(conf: StiltEnv, reservedCores: Int) = Props(classOf[WorkMaster], conf, reservedCores)

}
