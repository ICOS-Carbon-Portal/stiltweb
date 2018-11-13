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
		findUnfinishedJobs.foreach(startJob)
	}

	override def getStreamElement = state.getDashboardInfo

	override def specificReceive: Receive = coreReceive.andThen{_ =>
		for((w, slots) <- state.distributeWork() if !slots.isEmpty){
			w ! CalculateSlots(slots)
			log.debug(s"Sent ${slots.size} slots to ${w}")
		}
	}

	def coreReceive: Receive = {
		case jobRequest: Job =>
			val job = jobRequest.copySetStarted
			log.info(s"Receiving new job, saving it to ${jobsDir}/${job.id}")
			val jdir = JobDir.save(job, jobsDir.resolve(job.id))
			startJob(jdir)

		case CancelJob(id) => state.cancelJob(id).foreach{
			jdir => jdir.delete()
		}

		case wms @ WorkMasterStatus(_, totalCores) =>
			val wm = sender()
			if (! state.isKnownWorker(wm)) {
				log.info(s"Seeing new computational node $wm with $totalCores CPUs")
				context.watch(wm)
			}
			state.handleWorkerUpdate(wm, wms)

		case Terminated(watched) => if(state.isKnownWorker(watched)){
			state.removeWorker(watched)
		}

		case PleaseSendDashboardInfo =>
			sender() ! state.getDashboardInfo

		case SlotCalculated(result) => {
			log.debug("Got SlotCalculated, saving to the slot archive")
			state.slotArchiver.save(result)
			state.onSlotDone(result.slot)
		}

		case StiltFailure(slot) =>
			state.onSlotDone(slot)
	}

	def startJob(jdir: JobDir): Unit = {
		log.info(s"Starting job $jdir.job")
		state.startJob(jdir)
	}

	def findUnfinishedJobs: Iterator[JobDir] = {
		val pathIter = Util.iterateChildren(jobsDir)
		for(dir <- pathIter if JobDir.isUnfinishedJobDir(dir) ) yield JobDir(dir)
	}
}

object WorkReceptionist{
	def props(stateDir: Path, slotStepInMinutes: Integer) = Props.create(classOf[WorkReceptionist], stateDir: Path, slotStepInMinutes)
}
