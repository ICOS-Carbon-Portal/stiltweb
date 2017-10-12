package se.lu.nateko.cp.stiltcluster

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.{Actor, ActorLogging}
import se.lu.nateko.cp.stiltweb.StiltJsonSupport._
import spray.json._


class JobArchiver(archiveDirectory: File) extends Actor with ActorLogging {

	val receptionist = context.actorSelection("/user/receptionist")

	final val jobFileName = "job.json"
	log.info(s"JobArchiver starting up in ${archiveDirectory}")

	readOldJobsFromDisk

	def receive = {
		case PersistJob(job: Job) =>
			log.info(s"Asked to create job $job")
			val jobDir = new File(archiveDirectory, job.id)
			if (jobDir.isDirectory()) {
				log.warning(s"$jobDir already existed, ignoring")
			} else {
				if (! jobDir.mkdir())
					log.error(s"Could not create $jobDir")
				val f = new File(jobDir, "job.json")
				val w = new BufferedWriter(new FileWriter(f))
				w.write(job.toJson.prettyPrint)
				w.close
				log.info(s"Wrote job file $f")
				sender() ! BeginJob(job, jobDir.toString)
			}

		case JobFinished(job) => {
			val jobDir = new File(archiveDirectory, job.id)
			if (jobDir.isDirectory) {
				val done = new File(jobDir, "done")
				done.createNewFile()
			}
		}

	}

	private def readOldJobsFromDisk() = {
		val isJobDir  = { f:File =>
			f.isDirectory() && f.getName.startsWith("job_")
		}
		val jobNotDone = { f:File =>
			! (new File(f, "done").exists)
		}
		for(d <- archiveDirectory.listFiles.filter(isJobDir).filter(jobNotDone)) {
			val file = new File(d, "job.json")
			val json = scala.io.Source.fromFile(file).mkString.parseJson
			val job  = JobFormat.read(json)
			log.info(s"Read old job '${job}' and sending it to receptionist")
			receptionist ! BeginJob(job, d.toString)
		}
	}
}
