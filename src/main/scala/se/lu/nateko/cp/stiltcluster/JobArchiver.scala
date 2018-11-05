package se.lu.nateko.cp.stiltcluster

import java.nio.file.{ Files, Path }

import scala.collection.JavaConverters.asScalaIteratorConverter

import akka.actor.Actor

class JobArchiver(dataDir: Path) extends Actor with Trace {

	val receptionist = context.actorSelection("/user/receptionist")

	final val jobsDir = Util.ensureDirectory(dataDir.resolve("jobs"))

	protected final val traceFile = jobsDir.resolve("trace.log")

	def receive = {
		case PersistJob(job: Job) =>
			trace(s"Asked to create job $job")
			val dir = jobsDir.resolve(job.id)
			val jdir = JobDir.save(job, dir)
			trace(s"Wrote job file for $job")
			sender() ! BeginJob(jdir)
	}

	trace(s"Starting up, looking in ${jobsDir} for unfinished jobs")

	val pathIter = Files.list(jobsDir).iterator.asScala

	for(dir <- pathIter if JobDir.isUnfinishedJobDir(dir) ) {

		val jdir = JobDir(dir)

		trace(s"Have read old job '${jdir.job}', sending it to receptionist")

		receptionist ! BeginJob(jdir)
	}
}
