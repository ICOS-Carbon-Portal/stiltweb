package se.lu.nateko.cp.stiltcluster

import java.nio.file.Path

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Terminated
import se.lu.nateko.cp.stiltweb.state.State

class WorkReceptionist(stateDir: Path, slotStepInMinutes: Integer) extends StreamPublisher[DashboardInfo] with ActorLogging {

	val jobsDir = Util.ensureDirectory(stateDir.resolve("jobs"))
	val state = new State(stateDir, slotStepInMinutes.intValue)

	override def preStart() = {
		log.info(s"Starting up, looking in ${jobsDir} for unfinished jobs")

		Util.iterateChildren(jobsDir).filter(JobDir.isUnfinishedJobDir).foreach{
			dir => startJob(JobDir(dir).job)
		}
	}

	override def getStreamElement = state.getDashboardInfo

	override def specificReceive: Receive = coreReceive.andThen{_ =>
		for((w, command) <- state.distributeWork() if !command.slots.isEmpty){
			w ! command
			log.debug(s"Sent ${command.slots.size} slots to ${w}")
		}
	}

	def coreReceive: Receive = {
		case jobRequest: Job =>
			val jdir = JobDir.saveAsNew(jobRequest.copySetStarted, jobsDir)

			log.info(s"Received $jobRequest, saved to ${jdir.dir}")

			startJob(jdir.job)

		case CancelJob(id) => state.cancelJob(id) match{
			case Some(job) =>
				log.info(s"Cancelling $job")
				jobDir(job).delete()
			case None =>
				log.warning(s"Job with id '$id' not found, therefore cannot cancel it")
		}

		case wms: WorkMasterStatus =>
			val wm = sender()
			if (! state.isKnownWorker(wm)) {
				log.info(s"Seeing new computational node with ${wms.nCpusTotal} CPUs at ${wm.path}")
				context.watch(wm)
			}
			val lostWork = state.handleWorkerUpdate(wm, wms)
			if(!lostWork.isEmpty){
				log.warning(s"Work lost (and re-queued): " + lostWork.mkString(", "))
			}

		case Terminated(watched) if(state.isKnownWorker(watched)) =>
			log.info(s"Computational node terminated: ${watched.path}")
			state.removeWorker(watched)

		case PleaseSendDashboardInfo =>
			sender() ! state.getDashboardInfo

		case SlotCalculated(result) => {
			log.debug(s"Got ${result.slot} calculated, saving to the slot archive")
			state.slotArchiver.save(result)
			finishSlot(result.slot)
		}

		case StiltFailure(slot, errMsg, logsZip) =>
			state.onSlotFailure(slot, errMsg, logPathMaker(slot)).foreach{job =>
				logsZip.foreach(jobDir(job).saveLogs(slot, _))
			}
			finishSlot(slot)
	}

	def finishSlot(slot: StiltSlot): Unit = state.onSlotDone(sender(), slot).foreach(finishJob)

	def finishJob(job: Job): Unit = {
		log.info(s"Done: $job")
		jobDir(job).markAsDone()
	}

	def startJob(job: Job): Unit = {
		log.info(s"Starting $job")
		if(!state.startJob(job)) finishJob(job)
	}

	def jobDir(job: Job) = JobDir.existing(job, jobsDir)

	def logPathMaker(slot: StiltSlot)(job: Job) = stateDir.relativize(jobDir(job).logsPath(slot)).toString
}

object WorkReceptionist{
	def props(stateDir: Path, slotStepInMinutes: Integer) = Props.create(classOf[WorkReceptionist], stateDir: Path, slotStepInMinutes)
}
