package se.lu.nateko.cp.stiltcluster

import java.io.File

import akka.actor.{Actor, ActorLogging}
import se.lu.nateko.cp.stiltweb.StiltJsonSupport._
import spray.json._


class JobDir(val job: Job, val dir: File) {

	private val slotsFile = new File(dir, "slots.json")
	private val slotsDir = new File(dir, "slots")

	if (! slotsDir.isDirectory()) {
		slotsDir.mkdir()
	}

	private var _slots:Option[Seq[StiltSlot]] = loadSlots
	def slots = _slots

	def loadSlots(): Option[Seq[StiltSlot]] = {
		if (slotsFile.exists) {
			val json = scala.io.Source.fromFile(slotsFile).mkString.parseJson
			Some(json.convertTo[Seq[String]].map { job.getSlot(_) })
		} else {
			None
		}
	}

	def saveSlotList(slots: Seq[StiltSlot]): Unit = {
		// Write only the slot-strings to file, i.e "2012010209" ...
		Util.writeFileAtomically(slotsFile, slots.map { _.slot }.toJson.prettyPrint)
		this._slots = Some(slots)
	}

	def markAsDone() = {
		Util.createEmptyFile(dir, "done")
	}

	def linkSlot(slot: LocallyAvailableSlot) = {
		val link = new File(slotsDir, slot.slot)
		Util.createSymbolicLink(link, slot.file)
	}

	def slotPresent(s: StiltSlot): Boolean = {
		Util.fileExists(slotsDir, s.slot)
	}

	def findMissingSlots = {
		_slots.get.filterNot { slotPresent(_) }
	}
}



class JobArchiver(archiveDirectory: File) extends Actor with ActorLogging {

	val receptionist = context.actorSelection("/user/receptionist")

	final val jobFileName = "job.json"
	log.info(s"JobArchiver starting up in ${archiveDirectory}")

	readOldJobsFromDisk

	def receive = {
		case PersistJob(job: Job) =>
			log.info(s"Asked to create job $job")
			val dir = new File(archiveDirectory, job.id)
			if (dir.isDirectory) {
				log.warning(s"$dir already existed, ignoring")
			} else {
				if (! dir.mkdir()) {
					log.error(s"Could not create $dir")
				} else {
					val f = new File(dir, jobFileName)
					Util.writeFileAtomically(f, job.toJson.prettyPrint)
					log.info(s"Wrote job file $f")
					sender() ! new JobDir(job, dir)
				}
			}

		case JobFinished(jdir: JobDir) => {
			jdir.markAsDone
		}

	}

	private def readOldJobsFromDisk() = {
		val isJobDir = { f:File =>
			f.isDirectory() && f.getName.startsWith("job_")
		}
		val jobNotDone = { f:File =>
			! Util.fileExists(f, "done")
		}
		for(d <- archiveDirectory.listFiles.filter(isJobDir).filter(jobNotDone)) {
			val file = new File(d, jobFileName)
			val json = scala.io.Source.fromFile(file).mkString.parseJson
			val job  = JobFormat.read(json)
			val jdir = new JobDir(job, d)
			log.info(s"Read old job '${job}' and sending it to receptionist")
			receptionist ! BeginJob(jdir)
		}
	}
}
