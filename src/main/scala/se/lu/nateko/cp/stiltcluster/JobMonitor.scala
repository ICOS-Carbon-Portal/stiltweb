package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.Props
import java.nio.file.Path

class JobMonitor(jobDir: JobDir, mainDirectory: Path) extends Actor with Trace {

	val exposer = new ResultsExposer(mainDirectory)
	val slotCalculator = context.actorSelection("/user/slotcalculator")
	val slotProducer = context.actorSelection("/user/slotproducer")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	protected val traceFile = jobDir.dir.resolve("trace.log")

	override def preStart(): Unit = {
		trace(s"Starting up in ${jobDir.dir}")

		if (jobDir.slots.isEmpty) {
			trace("No slot list, requesting one.")
			slotCalculator ! CalculateSlotList(jobDir.job)
		} else {
			trace("Already have slot list")
			requestRemainingSlots()
		}
	}

	private val deletionHandler: Receive = {
		case deletion @ CancelJob(id) =>
			if(id == jobDir.job.id){
				dashboard ! deletion
				jobDir.delete()
				context stop self
			}
	}

	def receive = deletionHandler.orElse{
		case SlotListCalculated(slots) =>
			trace(s"Received a list of ${slots.length} slots")
			jobDir.saveSlotList(slots)
			requestRemainingSlots()
	}

	def requestRemainingSlots() = {
		val remaining = jobDir.missingSlots

		trace(s"$totalSlotsNum slots in total. ${remaining.length} remaining, sending request.")

		slotProducer ! RequestManySlots(remaining)
		workOnRemaining(remaining)
	}

	def totalSlotsNum = jobDir.slots.fold(0)(_.length)

	def workOnRemaining(remaining: Seq[StiltSlot]): Unit = {

		val totSlots = totalSlotsNum
		dashboard ! JobInfo(jobDir.job, totSlots, totSlots - remaining.length)

		if(remaining.isEmpty){
			trace(s"All slots computed, telling slot calculator to merge.")
			slotCalculator ! MergeJobDir(jobDir)
			context become merging
		} else
			context become workingOn(remaining)
	}

	def workingOn(outstanding: Seq[StiltSlot]): Receive = deletionHandler.orElse{
		case SlotAvailable(local) =>
			val (removed, remaining) = outstanding.partition(local.equals(_))

			if (removed.isEmpty) {
				trace(s"Received slot I'm not waiting for ${local}")
			} else {
				if (jobDir.slotPresent(local)) {
					trace("Received a slot that is already present")
				} else {
					trace(s"Received new slot, ${remaining.length} remaining.")
					jobDir.link(local)
				}
			}
			workOnRemaining(remaining)

		case StiltFailure(slot) =>
			workOnRemaining(outstanding.filter(_ != slot))
	}

	def merging(): Receive = {
		case JobDirMerged =>
			trace(s"Job directory merged. Exposing the job results for the STILT viewer")
			jobDir.markAsDone()
			exposer.expose(jobDir)
			dashboard ! JobFinished(JobInfo(jobDir.job, totalSlotsNum, totalSlotsNum))
			trace(s"Results exposed, dashboard notified, terminating.")
			context stop self
	}

}

object JobMonitor{
	def props(jdir: JobDir, mainDirectory: Path): Props = Props.create(classOf[JobMonitor], jdir, mainDirectory)
}
