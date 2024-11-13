package se.lu.nateko.cp.stiltcluster

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Terminated
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.stiltweb.AtmoAccessClient
import se.lu.nateko.cp.stiltweb.AtmoAccessClient.AppInfo
import se.lu.nateko.cp.stiltweb.JobDir
import se.lu.nateko.cp.stiltweb.state.Archiver
import se.lu.nateko.cp.stiltweb.state.State

import java.time.Instant

class WorkReceptionist(archiver: Archiver, atmoClient: AtmoAccessClient) extends StreamPublisher[DashboardInfo] with ActorLogging {

	val state = new State(archiver)

	override def preStart() = {
		log.info(s"Starting up, looking in ${archiver.jobsDir} for unfinished jobs")

		Util.iterateChildren(archiver.jobsDir).filter(JobDir.isUnfinishedJobDir).foreach{
			dir => startJob(JobDir.load(dir).job)
		}
	}

	override def getStreamElement = state.getDashboardInfo

	override def specificReceive: Receive =
		case jobRequest: Job =>
			val jdir = archiver.save(jobRequest.copySetStarted)

			log.info(s"Received $jobRequest, saved to ${jdir.dir}")

			startJob(jdir.job)
			distributeWork()

		case CancelJob(id) => state.cancelJob(id) match
			case Some(job) =>
				log.info(s"Cancelling $job")
				jobDir(job).delete()
			case None =>
				log.warning(s"Job with id '$id' not found, therefore cannot cancel it")

		case wms: WorkMasterStatus =>
			val wm = sender()
			wm ! Thanks
			if (! state.isKnownWorker(wm)) {
				log.info(s"Seeing new computational node with ${wms.nCpusTotal} CPUs at ${wm.path}")
				context.watch(wm)
			}
			val lostWork = state.handleWorkerUpdate(wm, wms)
			if(!lostWork.isEmpty){
				log.warning(s"Work lost by $wm, re-queued again: " + lostWork.mkString(", "))
			}
			distributeWork()

		case Terminated(watched) if(state.isKnownWorker(watched)) =>
			log.info(s"Computational node terminated: ${watched.path}")
			state.removeWorker(watched)

		case PleaseSendDashboardInfo =>
			sender() ! state.getDashboardInfo

		case SlotCalculated(result) =>
			log.debug(s"Got ${result.slot} calculated, saving to the slot archive")
			archiver.save(result)
			finishSlot(result.slot)
			distributeWork()

		case StiltFailure(slot, errMsgs, logsZip) =>
			state.onSlotFailure(slot, errMsgs, logPathMaker(slot)).foreach{job =>
				logsZip.foreach(jobDir(job).saveLogs(slot, _))
			}
			finishSlot(slot)

		case DistributeWork =>
			distributeWork()

	end specificReceive


	def finishSlot(slot: StiltSlot): Unit = state.onSlotDone(sender(), slot).foreach(finishJob)

	def finishJob(job: Job): Unit =
		log.info(s"Done: $job")
		for startD <- job.timeStarted do atmoClient.log:
			import atmoClient.baseStiltUrl
			AppInfo(
				user = UserId(job.userId),
				startDate = startD,
				endDate = job.timeStopped.orElse(Some(Instant.now)),
				resultUrl = s"$baseStiltUrl/viewer/?stationId=${job.siteId}&fromDate=${job.start}&toDate=${job.stop}",
				infoUrl = None,
				comment = Some(s"STILT run for station ${job.siteId} (lat = ${job.lat}, lon = ${job.lon}) from ${job.start} to ${job.stop}")
			)
		jobDir(job).markAsDone()


	def startJob(job: Job): Unit =
		log.info(s"Starting $job")
		val thereIsWorkToBeDone = state.startJob(job)
		if(!thereIsWorkToBeDone) finishJob(job)

	def distributeWork(): Unit = for (w, command) <- state.distributeWork() do
		w ! command
		log.debug(s"Sent ${command.slots.size} slots to ${w}")

	def jobDir(job: Job) = new JobDir(job, archiver.getJobDir(job))

	def logPathMaker(slot: StiltSlot)(job: Job) = archiver.stateDir.relativize(jobDir(job).logsPath(slot)).toString
}

object WorkReceptionist:
	def props(archiver: Archiver, atmoClient: AtmoAccessClient) = Props.create(classOf[WorkReceptionist], archiver, atmoClient)
