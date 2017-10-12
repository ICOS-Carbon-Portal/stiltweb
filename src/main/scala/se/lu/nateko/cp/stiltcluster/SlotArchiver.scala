package se.lu.nateko.cp.stiltcluster

import java.io.File
import java.nio.file.{ Files, Path, Paths }

import akka.actor.{Actor, ActorLogging}


class SlotArchiver(archiveDirectory: File) extends Actor with ActorLogging {

	final val slotsDir = Paths.get(archiveDirectory.toString).resolve("slots")
	log.info(s"JobArchiver starting up in ${slotsDir}")

	def receive = {
		case SlotCalculated(job, slot) =>
			log.info(s"Slot calculated ${job}/${slot}")
			val jobDir = jobToDir(job)
			Files.createDirectory(jobDir)
			val data = jobDir.resolve(slot)
			log.info(s"Touching file ${data}")
			Files.createFile(data)
			sender() ! SlotAvailable(job, slot, data)

		case SlotRequest(job, slot) =>
			val data = jobToDir(job).resolve(slot)
			if (Files.exists(data))
				sender() ! SlotAvailable(job, slot, data)
			else
				sender() ! SlotUnAvailable(job, slot)


			// val out = jobDir.resolve("blob.zip")
			// Files.write(out, blob,
			//			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
			// log.info(s"Wrote ${blob.size} bytes to ${out}")
	}

	private def jobToDir(job: Job): Path = {
		slotsDir.resolve(s"lat${job.lat}-lon${job.lon}-alt${job.alt}")
	}
}
