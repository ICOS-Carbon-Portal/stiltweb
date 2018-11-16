package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}

import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
import spray.json._


class JobDir(val job: Job, val dir: Path) {

	def markAsDone(): Unit = {
		val doneFile = dir.resolve(JobDir.DoneFile)
		if(!Files.exists(doneFile)) Util.writeFileAtomically(doneFile, Array.empty[Byte])
	}

	def logsPath(slot: StiltSlot) = dir.resolve(s"logs_$slot.zip")
	def saveLogs(slot: StiltSlot, logZip: Array[Byte]) = Util.writeFileAtomically(logsPath(slot), logZip)

	def delete(): Unit =
		try{
			Util.deleteDirRecursively(dir)
		}catch{
			case _: Throwable =>
		}
}

object JobDir{

	val JobFile = "job.json"
	val DoneFile = "done"

	def isJobDir(f: Path) = Files.isDirectory(f) && Files.exists(f.resolve(JobFile))
	def isUnfinishedJobDir(f: Path) = isJobDir(f) && ! Files.exists(f.resolve(DoneFile))

	def apply(dir: Path) = {
		val jobFile = dir.resolve(JobFile)
		val job = scala.io.Source.fromFile(jobFile.toFile).mkString.parseJson.convertTo[Job]
		new JobDir(job, dir)
	}

	def save(job: Job, toJobsDir: Path): JobDir = {
		val dir = resolvePath(toJobsDir, job)
		Files.createDirectories(dir)
		val f = dir.resolve(JobFile)
		Util.writeFileAtomically(f, job.toJson.prettyPrint)
		new JobDir(job, dir)
	}

	def existing(job: Job, jobsDir: Path) = new JobDir(job, resolvePath(jobsDir, job))

	def resolvePath(jobsDir: Path, job: Job): Path = jobsDir.resolve(job.id)

}
