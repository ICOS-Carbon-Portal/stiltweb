package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.Props
import java.time.LocalDateTime
import java.time.LocalTime
import java.nio.file.Files

class JobMonitor(jobDir: JobDir, slotStepInMinutes: Integer) extends Actor with Trace {

	val slotProducer = context.actorSelection("/user/slotproducer")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	protected val traceFile = jobDir.dir.resolve("trace.log")

	override def preStart(): Unit = {
		trace(s"Starting up in ${jobDir.dir}")

		if (jobDir.slots.isEmpty) {
			trace("No slot list, calculating.")
			val slots = JobMonitor.calculateSlots(jobDir.job, slotStepInMinutes)
			jobDir.saveSlotList(slots)
		} else
			trace("Already have slot list")

		requestRemainingSlots()
	}

	private val deletionHandler: Receive = {
		case deletion @ CancelJob(id) =>
			if(id == jobDir.job.id){
				jobDir.slots.foreach{slots =>
					slotProducer ! CancelSlots(slots)
				}
				dashboard ! deletion
				jobDir.delete()
				context stop self
			}
	}

	def receive = deletionHandler

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
			trace(s"All slots computed, finishing the job.")
			jobDir.markAsDone()
			JobMonitor.ensureStationIdLinkExists(jobDir)
			dashboard ! JobFinished(JobInfo(jobDir.job.copySetStopped, totalSlotsNum, totalSlotsNum))
			trace(s"Job done, dashboard notified, terminating.")
			context stop self
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

}

object JobMonitor{

	def props(jdir: JobDir, slotStepInMinutes: Integer): Props = Props.create(classOf[JobMonitor], jdir, slotStepInMinutes)

	def calculateSlots(job: Job, stepInMinutes: Int): Seq[StiltSlot] = {
		val start = LocalDateTime.of(job.start, LocalTime.MIN)
		val stop = LocalDateTime.of(job.stop, LocalTime.MIN).plusDays(1)

		Iterator.iterate(start)(_.plusMinutes(stepInMinutes.toLong))
			.takeWhile(_.compareTo(stop) < 0)
			.map{dt =>
				val time = StiltTime(dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour)
				val pos = StiltPosition(job.lat, job.lon, job.alt)
				StiltSlot(time, pos)
			}
			.toIndexedSeq
	}

	def ensureStationIdLinkExists(jdir: JobDir): Unit = {
		val stationIdLink = jdir.dir.resolve("../../stations/" + jdir.job.siteId).toAbsolutePath

		if(!Files.exists(stationIdLink)){
			jdir.slots.flatMap(_.headOption).foreach{slot =>
				val target = jdir.dir.resolve("../../slots/" + slot.pos.toString).toAbsolutePath
				Files.createSymbolicLink(stationIdLink, target)
			}
		}
	}
}
