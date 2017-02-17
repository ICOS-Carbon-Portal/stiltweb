package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Address
import akka.actor.Terminated
import java.time.LocalDate
import scala.collection.mutable.Map
import scala.collection.mutable.Queue
import scala.collection.mutable.Buffer
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

class WorkReceptionist extends Actor{
	import WorkReceptionist._

	private val nodes = Map.empty[ActorRef, WorkMasterStatus]
	private val queue = Queue.empty[Job]
	private val done = Buffer.empty[JobInfo]

	override def preStart(): Unit = {
		context.system.scheduler.schedule(2 seconds, 2 seconds, self, CollectStatus)(context.system.dispatcher)
	}

	val log = context.system.log

	def receive = {

		case job: Job =>
			queue.enqueue(job)
			dispatchJob()

		case msg @ CancelJob(id) =>
			findNodeByJob(id).foreach(_ ! msg)

		case CollectStatus =>
			nodes.keys.foreach(_ ! GetStatus)

		case GetStatus =>
			sender() ! getDashboardInfo

		case wms: WorkMasterStatus =>
			val workMaster = sender()
			nodes += ((workMaster, wms))

			val completed = wms.work.collect{
				case (run, status) if status.exitValue.isDefined =>
					JobInfo(run, status, workMaster.path.address)
			}
			done ++= completed

			if(!completed.isEmpty) workMaster ! Thanks(completed.map(_.status.id))
			if(wms.freeCores > 0) dispatchJob()

		case run: JobRun =>
			val oldStatus = nodes(sender())
			val newStatus = oldStatus.copy(
				work = oldStatus.work :+ ((run, JobStatus.init(run.runId)))
			)
			nodes += ((sender(), newStatus))
			log.info("STARTED STILT RUN: " + run.toString)
			dispatchJob()

		case StopAllWork =>
			nodes.keys.foreach(_ ! StopAllWork)
			context stop self

		case WorkMasterRegistration(nCores) if ! nodes.contains(sender()) =>
			val workMaster = sender()
			context watch workMaster
			nodes += ((workMaster, WorkMasterStatus(Nil, nCores)))
			workMaster ! GetStatus

			log.info("WORK MASTER REGISTERED: " + workMaster)
			log.info("CURRENT WORK MASTER COUNT: " + nodes.keys.size)
			dispatchJob()

		case Terminated(wm) =>
			log.info("WORK MASTER UNREGISTERED: " + wm)
			nodes -= wm
	}

	private def dispatchJob(): Unit = if(!queue.isEmpty){
		pickNodeForJob(nodes, queue.head) match{
			case Some(node) =>
				val job = queue.dequeue()
				node ! job
			case None =>
		}
	}

	private def getDashboardInfo = DashboardInfo(
		running = nodes.flatMap{
			case (node, WorkMasterStatus(work, _)) =>
				work.collect{
					case (run, status) if status.exitValue.isEmpty => JobInfo(run, status, node.path.address)
				}
		}.toSeq,
		done = done,
		queue = queue
	)

	private def findNodeByJob(jobId: String): Option[ActorRef] = nodes.keys.find{
		node => nodes(node).work.exists(_._1.runId == jobId)
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
