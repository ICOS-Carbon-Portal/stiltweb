package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}

import akka.actor.{Actor, ActorLogging}
import spray.json._
import se.lu.nateko.cp.stiltweb.StiltJsonSupport._


class JobDir(val job: Job, val dir: Path) {

	private val slotsFile = dir.resolve("slots.json")
	private val slotsDir = dir.resolve("slots")

	Util.ensureDirectory(slotsDir)

	private var _slots:Option[Seq[StiltSlot]] = loadSlots
	def slots = _slots

	private def loadSlots(): Option[Seq[StiltSlot]] = {
		if (Files.exists(slotsFile)) {
			val json = scala.io.Source.fromFile(slotsFile.toFile).mkString.parseJson
			Some(json.convertTo[Seq[StiltSlot]])
		} else {
			None
		}
	}

	def saveSlotList(slots: Seq[StiltSlot]): Unit = {
		Util.writeFileAtomically(slotsFile.toFile, slots.toJson.prettyPrint)
		this._slots = Some(slots)
	}

	def markAsDone() = {
		Util.createEmptyFile(dir.toFile, "done")
	}

	def link(local: LocallyAvailableSlot) = {
		local.link(this.slotsDir)
	}

	def slotPresent(s: StiltSlot): Boolean = {
		LocallyAvailableSlot.isLinked(dir, s)
	}

	def slotPresent(s: LocallyAvailableSlot): Boolean = {
		LocallyAvailableSlot.isLinked(dir, s.slot)
	}

	def missingSlots = {
		_slots.get.filterNot { slotPresent(_) }
	}
}



class JobArchiver(dataDir: Path) extends Actor with ActorLogging {

	val receptionist = context.actorSelection("/user/receptionist")

	final val jobsDir = dataDir.resolve("jobs")
	Util.ensureDirectory(jobsDir)

	final val jobFile = "job.json"
	log.info(s"starting up in ${jobsDir}")

	readOldJobsFromDisk

	def receive = {
		case PersistJob(job: Job) =>
			log.info(s"Asked to create job $job")
			val dir = jobsDir.resolve(job.id)
			if (Files.isDirectory(dir)) {
				log.warning(s"$dir already existed, ignoring")
			} else {
				Files.createDirectory(dir)
				val f = dir.resolve(jobFile)
				Util.writeFileAtomically(f.toFile, job.toJson.prettyPrint)
				log.info(s"Wrote job file $f")
				sender() ! BeginJob(new JobDir(job, dir))
			}

		case JobFinished(jdir: JobDir) => {
			jdir.markAsDone
		}
	}

	private def readOldJobsFromDisk() = {
		import scala.collection.JavaConverters._

		log.info(s"Looking in ${jobsDir} for unfinished jobs")
		val isJobDir   = { f:Path => Files.isDirectory(f) && Files.exists(f.resolve("job.json")) }
		val jobNotDone = { f:Path => ! Util.fileExists(f.toFile, "done") }

		for (d <- Files.list(jobsDir).iterator.asScala) {
			log.info(s"JobArchiver looking at ${d} isJobDir=${isJobDir(d)} jobNotDone=${jobNotDone(d)}")
		}
		// FIXME: Couldn't get this to work without the .iterator.asScala dance :(
		val i = Files.list(jobsDir).iterator.asScala
		for(d <- i.filter(isJobDir).filter(jobNotDone) ) {
			val file = d.resolve(jobFile)
			val json = scala.io.Source.fromFile(file.toFile).mkString.parseJson
			val job  = JobFormat.read(json)
			val jdir = new JobDir(job, d)
			log.info(s"Read old job '${job}' and sending it to receptionist")
			receptionist ! BeginJob(jdir)
		}
	}
}
