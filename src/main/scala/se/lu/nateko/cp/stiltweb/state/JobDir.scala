package se.lu.nateko.cp.stiltweb

import java.nio.file.{Files, Path}
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport.JobFormat
import spray.json._
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltcluster.StiltSlot
import se.lu.nateko.cp.stiltcluster.Util


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

	def load(dir: Path): JobDir = {
		val jobFile = dir.resolve(JobFile)
		val job = scala.io.Source.fromFile(jobFile.toFile).mkString.parseJson.convertTo[Job]
		new JobDir(job, dir)
	}

	def saveAsNew(job: Job, dir: Path): JobDir = {
		Files.createDirectories(dir)

		val f = dir.resolve(JobFile)
		Util.writeFileAtomically(f, job.toJson.prettyPrint)

		val previouslyDoneFile = dir.resolve(DoneFile)
		//user may want to re-run same job again because of some failed slots
		Files.deleteIfExists(previouslyDoneFile)

		new JobDir(job, dir)
	}

}
