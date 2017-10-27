package se.lu.nateko.cp.stiltcluster

import scala.collection.immutable.Seq
import scala.collection.mutable.Map

case object Hi

case class CancelJob(id: String)

case class JobCanceled(id: String)

case class StiltSlot(lat: Double, lon: Double, alt: Int, slot: String) {
	// ex: "2012010309"
	private val slotRE = """(\d{4})(\d{2})(\d{2})(\d{2})""".r

	val slotRE(year, month, day, hour) = slot

}


trait StiltSlotResult {
	val slot: StiltSlot

	def iterator: Iterator[(String, Array[Byte])]
}


class StiltSlotResultMap (val slot: StiltSlot) extends StiltSlotResult {

	private val files = Map[String, Array[Byte]]()

	def iterator = files.iterator

	def addFile(path: String, data: Array[Byte]) = files.update(path, data)
}

case class CalculateSlot(slot: StiltSlot)
case class SlotCalculated(slot: StiltSlot)
case class WorkMasterStatus(nCpusFree: Int)
case class Thanks(ids: Seq[String])
