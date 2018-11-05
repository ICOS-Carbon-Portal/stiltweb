package se.lu.nateko.cp.stiltcluster

import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime

import akka.actor.Actor
import akka.actor.Props

class JobMonitor(jobDir: JobDir, slotStepInMinutes: Integer) extends Actor with Trace {

	val slotProducer = context.actorSelection("/user/slotproducer")
	val dashboard = context.actorSelection("/user/dashboardmaker")

	val allSlots = JobMonitor.calculateSlots(jobDir.job, slotStepInMinutes)

	protected val traceFile = jobDir.dir.resolve("trace.log")

	override def preStart(): Unit = {

		trace(s"Starting up the job, ${allSlots.size} slots in total, sending request.")

		slotProducer ! RequestManySlots(allSlots)
		workOnRemaining(allSlots)
	}

	private def deletionHandler(slots: Seq[StiltSlot]): Receive = {
		case deletion @ CancelJob(id) =>
			if(id == jobDir.job.id){
				slotProducer ! CancelSlots(slots)
				dashboard ! deletion
				jobDir.delete()
				context stop self
			}
	}

	def receive = deletionHandler(allSlots)

	def workOnRemaining(remaining: Seq[StiltSlot]): Unit = {

		val totalSlotsNum = allSlots.size
		dashboard ! JobInfo(jobDir.job, totalSlotsNum, totalSlotsNum - remaining.length)

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

	def workingOn(outstanding: Seq[StiltSlot]): Receive = deletionHandler(outstanding).orElse{
		case SlotAvailable(local) =>
			val (removed, remaining) = outstanding.partition(local.equals(_))

			if (removed.isEmpty) trace(s"Received slot I'm not waiting for ${local}")
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
				StiltSlot(time, job.pos)
			}
			.toIndexedSeq
	}

	def ensureStationIdLinkExists(jdir: JobDir): Unit = {
		val stationIdLink = jdir.dir.resolve("../../stations/" + jdir.job.siteId).normalize

		if(!Files.exists(stationIdLink)){
			val target = Paths.get("../slots/" + jdir.job.pos.toString)
			Files.createSymbolicLink(stationIdLink, target)
		}
	}
}
