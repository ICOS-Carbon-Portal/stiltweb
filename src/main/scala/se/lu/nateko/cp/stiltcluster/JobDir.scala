package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}
import java.util.Comparator

import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
import spray.json._


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
		LocallyAvailableSlot.isLinked(slotsDir, s)
	}

	def slotPresent(s: LocallyAvailableSlot): Boolean = {
		slotPresent(s.slot)
	}

	def missingSlots = {
		_slots.get.filterNot { slotPresent(_) }
	}

	def delete(): Unit =
		try{
			Files.walk(dir)
				.sorted(Comparator.reverseOrder[Path])
				.forEach(_.toFile.delete())
		}catch{
			case _: Throwable =>
		}
}
