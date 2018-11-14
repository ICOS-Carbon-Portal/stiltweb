package se.lu.nateko.cp.stiltcluster

case class CalculateSlots(requestId: Long, slots: Seq[StiltSlot])

case class SlotCalculated(result: StiltResult)

case class StiltFailure(slot: StiltSlot)

case class WorkMasterStatus(nCpusTotal: Int, work: Seq[StiltSlot], respondingToRequest: Option[Long] = None){
	def freeCores = nCpusTotal - work.size
}

