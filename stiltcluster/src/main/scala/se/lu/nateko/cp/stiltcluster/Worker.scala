package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.Props
import java.nio.file.Paths
import java.nio.file.Files

class Worker(slot: StiltSlot) extends Actor with Trace{

	import Worker.StiltOutput

	protected val traceFile = Paths.get("workmaster.log")
	private var r: StiltResult = _

	override def preStart() = runStilt()
	//To avoid postStop() being called during restart:
	override def preRestart(reason: Throwable, message: Option[Any]): Unit = {}

	override def postStop() = {
		val msg = if(r == null) StiltFailure(slot) else SlotCalculated(r)
		trace(s"Slot calculation attempt(s) resulted in  $msg")
		context.parent ! msg
	}

	override def receive = {
		case StiltOutput(s) =>
			trace(s"Stilt simulation finished $slot ($s)")
			val d = Paths.get(s)
			assert(Files.isDirectory(d))
			r = StiltResult(slot, d.resolve("output"))
			context stop self
	}

	private def runStilt(): Unit = scala.concurrent.ExecutionContext
		.Implicits.global.execute(() => {
			trace(s"Starting stilt calculation of $slot")
			self ! StiltOutput(RunStilt.cmd_run(slot))
		})
}

object Worker{
	def props(slot: StiltSlot) = Props.create(classOf[Worker], slot)

	private case class StiltOutput(s: String)
}
