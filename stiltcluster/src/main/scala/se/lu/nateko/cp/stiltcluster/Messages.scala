package se.lu.nateko.cp.stiltcluster

case object Hi
case class CancelJob(id: String)
case class JobCanceled(id: String)
case class CalculateSlots(slots: Seq[StiltSlot])
case class SlotCalculated(result: StiltResult)
case class WorkMasterStatus(nCpusTotal: Int, work: Seq[StiltSlot])

