package se.lu.nateko.cp.stiltcluster

case class CalculateSlots(requestId: Long, slots: Seq[StiltSlot])

sealed trait StiltOutcome
case class SlotCalculated(result: StiltResult) extends StiltOutcome

case class StiltFailure(slot: StiltSlot, errorMsgs: Seq[String], logsZip: Option[Array[Byte]]) extends StiltOutcome

case class WorkMasterStatus(nCpusTotal: Int, work: Seq[StiltSlot], respondingToRequest: Option[Long] = None){
	def freeCores = nCpusTotal - work.size
}

case object Thanks

