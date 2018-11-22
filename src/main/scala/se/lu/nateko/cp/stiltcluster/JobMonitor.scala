package se.lu.nateko.cp.stiltcluster

import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime

object JobMonitor{

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
