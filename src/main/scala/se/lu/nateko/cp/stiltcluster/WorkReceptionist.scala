package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.Map
import scala.collection.mutable.Queue
import scala.collection.mutable.Set

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.stream.actor.ActorPublisherMessage.Cancel

class WorkReceptionist extends Actor{
	import WorkReceptionist._

	private val nodes = Map.empty[ActorRef, WorkMasterStatus]
	private val queue = Queue.empty[Job]
	private val done = Set.empty[JobInfo]

	private val subscribers = Set.empty[ActorRef]

	val log = context.system.log

	def receive = {

		case Subscribe =>
			val streamPublisher = sender()
			subscribers += streamPublisher
			streamPublisher ! getDashboardInfo

		case Cancel =>
			subscribers -= sender()

		case job: Job =>
			log.info("WorkReceptionist receiving job")
			queue.enqueue(job)
			//if all workmasters are busy inform the clients about the queue increase:
			if(!dispatchJob()) {
				log.info("WorkReceptionist not dispatching job, all busy")
				notifySubscribers()
			}

		case msg @ CancelJob(id) =>
			findNodeByJob(id).foreach(_ ! msg)
			if(!queue.dequeueAll(_.id == id).isEmpty) notifySubscribers()

		case wms: WorkMasterStatus =>
			val workMaster = sender()

			if(!nodes.contains(workMaster)){
				workMaster ! Hi
				context watch workMaster
				log.info("WORK MASTER REGISTERED: " + workMaster)
				log.info("NEW WORK MASTER COUNT: " + (nodes.keys.size + 1))
			}

			nodes += ((workMaster, wms))

			log.info(s"WorkReceptionist receiving workmaster status, freeCores = ${wms.freeCores}, work=${wms}")
			val completed = wms.work.collect{
				case (run, status) if status.exitValue.isDefined =>
					JobInfo(run, status, workMaster.path.address)
			}
			done ++= completed

			if(!completed.isEmpty) workMaster ! Thanks(completed.map(_.status.id))

			queue.dequeueAll(wms.isRunning)

			if(wms.freeCores > 0) dispatchJob()
			notifySubscribers()

		case PleaseSendDashboardInfo =>
			log.error("WorkReceptionist receiving request for dashboardinfo")
			sender ! getDashboardInfo

		case StopAllWork =>
			nodes.keys.foreach(_ ! StopAllWork)
			context stop self

		case Terminated(wm) =>
			log.info("WORK MASTER UNREGISTERED: " + wm)
			nodes -= wm
			notifySubscribers()
	}

	private def notifySubscribers(): Unit = {
		log.info("WorkReceptionist notifying subscribers")
		val info = getDashboardInfo
		subscribers.foreach(_ ! info)
	}

	private def dispatchJob(): Boolean = (
		for(job <- queue.headOption; node <- pickNodeForJob(nodes, job)) yield {
			queue.dequeue()
			node ! job
		}
	).isDefined

	private def getDashboardInfo = DashboardInfo(
		running = nodes.flatMap{
			case (node, WorkMasterStatus(work, _)) =>
				work.collect{ case (job, status) if status.exitValue.isEmpty =>
					JobInfo(job, status, node.path.address)
				}
		}.toVector,
		done = done.toVector,
		queue = queue.toVector
	)

	private def findNodeByJob(jobId: String): Option[ActorRef] = nodes.keys.find{
		node => nodes(node).work.exists{
			case (job, _) => job.id == jobId
		}
	}

}

object WorkReceptionist{
	import java.time.temporal.ChronoUnit

	/**
	 * Cost of the job, in number of footprints
	 */
	def cost(job: Job): Int = {
		val days = (job.start.until(job.stop, ChronoUnit.DAYS) + 1).toInt
		days * 8 //1 footprints per 3 hours
	}

	def pickNodeForJob(nodes: Map[ActorRef, WorkMasterStatus], job: Job): Option[ActorRef] = {
		val maxPar = Math.max(cost(job) / 20, 1) //maximal reasonable parallelism for the job

		nodes.foldLeft[Option[(ActorRef, Int)]](None){

			case (acc, (actor, WorkMasterStatus(_, nextCores))) =>
				val next = Some((actor, nextCores))

				acc match{
					case None if nextCores > 0 => next
					case None => None

					case current @ Some((_, currentCores)) =>
						if(
							nextCores > 0 &&
							nextCores < maxPar &&
							(nextCores > currentCores || currentCores > maxPar)
						) next
						else current
				}
		}.map(_._1)

	}
}
