package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Paths}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process._

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, Terminated}


/* Tracks a remote SlotProducer actor.

 Sends an Identify message using an ActorSelection and then use ActorRef from
 the response.
 */
trait TrackSlotProducer extends Actor {

	case object Tick
	final val tickInterval = 5 seconds
	var producer: ActorRef = context.system.deadLetters
	final val ping = context.actorSelection(producerAddress)

	scheduleTick

	def trackSlotProducer: Receive = {
		case ActorIdentity(_, Some(ref)) =>
			if (ref != producer) {
				context.watch(ref)
				producer = ref
				producerFound
			}

		case Terminated(_) =>
			producer = context.system.deadLetters
			producerDied

		case Tick =>
			ping ! Identify(1)
			scheduleTick
	}

	private def scheduleTick() = {
		context.system.scheduler.scheduleOnce(tickInterval, self, Tick)
	}

	def producerAddress(): String
	def producerFound(): Unit
	def producerDied(): Unit
}


class WorkMaster(nCores: Int, prodAddr: String) extends Trace with TrackSlotProducer {

	private var freeCores = nCores
	protected val traceFile = Paths.get("workmaster.log")

	trace("WorkMaster starting up")

	def receive = slotCalculation orElse trackSlotProducer

	def slotCalculation: Receive = {
		case CalculateSlot(slot: StiltSlot) =>
			if (freeCores <= 0)
				trace("Received CalculateSlot even though I'm busy")
			else {
				freeCores -= 1
				calculateSlot(slot)
			}
			sender() ! myStatus

		case WorkMaster.Tick =>
			producer ! myStatus

		case WorkMaster.Stop =>
			trace(s"Terminated (was $self)")
			context stop self
	}

	private def finishSlot(msg: Any): Unit = {
		producer ! msg
		freeCores += 1
		producer ! myStatus
	}

	def producerFound() = {
		trace(s"New slotproducer detected, sending greeting (${freeCores} free cores)")
		producer ! myStatus
	}

	def producerDied() = {
		trace("Slotproducer died")
	}

	def producerAddress = prodAddr

	private def myStatus = WorkMasterStatus(freeCores, nCores)

	private def calculateSlot(slot: StiltSlot): Unit = {


		def calculateOnce(): Future[Unit] = Future{
			trace(s"Starting stilt calculation of $slot")

			val stiltOutput = RunStilt.cmd_run(slot)

			trace(s"Stilt simulation finished $slot ($stiltOutput)")

			val d = Paths.get(stiltOutput)
			assert(Files.isDirectory(d))

			finishSlot(SlotCalculated(StiltResult(slot, d.resolve("output"))))

			s"rm -rf -- ${d}".!!
			trace(s"Removed the ${d} directory")
			trace(s"Slot calculation for $slot was a success")
		}

		calculateOnce().recoverWith{ case _: Throwable => calculateOnce()}
			.failed
			.foreach{err =>
				trace(s"Slot calculation for $slot was a failure: " + err.getMessage)
				trace(err.getStackTrace().mkString("", "\n", "\n"))
				finishSlot(StiltFailure(slot))
			}
	}
}

object WorkMaster{
	def props(nCores: Int) = Props.create(classOf[WorkMaster], Int.box(nCores))

	case object Stop
	case object Tick
}
