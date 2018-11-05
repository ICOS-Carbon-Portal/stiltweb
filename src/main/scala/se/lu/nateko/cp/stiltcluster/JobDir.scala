package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}
import java.util.Comparator

import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
import spray.json._


class JobDir(val job: Job, val dir: Path) {

	def markAsDone(): Unit = Files.createFile(dir.resolve(JobDir.DoneFile))

	def delete(): Unit =
		try{
			Files.walk(dir)
				.sorted(Comparator.reverseOrder[Path])
				.forEach(_.toFile.delete())
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

	def save(job: Job, dir: Path) = {
		if (!Files.isDirectory(dir)) Files.createDirectory(dir)
		val f = dir.resolve(JobFile)
		Util.writeFileAtomically(f, job.toJson.prettyPrint)
		new JobDir(job, dir)
	}
}
