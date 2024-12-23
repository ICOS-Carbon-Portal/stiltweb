package se.lu.nateko.cp.stiltweb.state

import se.lu.nateko.cp.stiltcluster.WorkMasterStatus

import scala.collection.mutable.Map
import se.lu.nateko.cp.stiltcluster.StiltSlot

class WorkmasterState(initStatus: WorkMasterStatus) {

	import WorkmasterState._

	private var reportedFreeCores: Int = 0
	private var reportedTotalCores: Int = 0
	private var reportedActiveWork = Seq.empty[StiltSlot]
	private val requests: Map[RequestId, Request] = Map.empty
	private var totalRequestedWork: Long = 0
	private var totalLostWork: Long = 0

	updateAndGetLostWork(initStatus)

	def freeCores = Math.max(reportedFreeCores - requests.values.map(_.work.size).sum, 0)
	def totalCores = reportedTotalCores

	def requestWork(work: Seq[StiltSlot]): RequestId = {
		val id = getRequestId
		requests.update(id, new Request(work))
		totalRequestedWork += work.size
		id
	}

	def updateAndGetLostWork(wms: WorkMasterStatus): Seq[StiltSlot] = {
		reportedFreeCores = wms.freeCores
		reportedTotalCores = wms.nCpusTotal
		val previouslyReportedWork = reportedActiveWork
		reportedActiveWork = wms.work

		requests.values.foreach{req => req.age += 1}

		val purgedRequests: Seq[RequestId] = wms.respondingToRequest.toSeq ++ requests.collect{
			case (id, req) if req.age > RequestAgeLimit => id
		}

		val purgedWork: Seq[StiltSlot] = purgedRequests.map(requests.get).flatten.flatMap(_.work)

		requests.filterInPlace{
			case (id, _) => ! purgedRequests.contains(id)
		}

		val lost = (purgedWork ++ previouslyReportedWork).distinct.diff(wms.work)
		totalLostWork += lost.size
		lost
	}

	def onSlotDone(slot: StiltSlot): Unit = {
		reportedActiveWork = reportedActiveWork.filterNot(_ === slot)
	}

	def unfinishedWork: Seq[StiltSlot] = (reportedActiveWork ++ requests.values.flatMap(_.work)).distinct

	def isBadWorker: Boolean = totalRequestedWork > 100 && (totalLostWork > 0.7 * totalRequestedWork)
}

object WorkmasterState{
	val RequestAgeLimit = 5
	type RequestId = Long

	private[this] var reqId: RequestId = 0

	def getRequestId: RequestId = {
		reqId += 1
		reqId
	}

	private class Request(val work: Seq[StiltSlot]){
		var age = 0
	}
}