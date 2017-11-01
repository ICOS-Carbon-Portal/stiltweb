package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.Map

case object Hi

case class CancelJob(id: String)

case class JobCanceled(id: String)


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
