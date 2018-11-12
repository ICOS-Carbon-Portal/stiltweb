package se.lu.nateko.cp.stiltcluster

import java.nio.file.{ Files, Paths }

import scala.collection.mutable.Set
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

import akka.actor.{ Actor, ActorRef, Props, Terminated }
import akka.actor.ActorLogging
import akka.util.Timeout


class WorkMaster(nCores: Int, prodAddr: String) extends Actor with ActorLogging {

	var producer: ActorRef = context.system.deadLetters
	val work = Set.empty[StiltSlot]

	log.info("WorkMaster starting up")

	override def preStart(): Unit = findSlotProducer()

	def findSlotProducer(): Unit = {
		implicit val timeout = Timeout(3.seconds)
		import context.dispatcher

		def findIt(): Unit = context.actorSelection(prodAddr).resolveOne().onComplete{
			case Success(ref) =>
				context.watch(ref)
				log.info(s"Found a slot producer $ref")
				producer = ref
				ref ! myStatus
			case _ =>
				context.system.scheduler.scheduleOnce(1.second)(findIt())
		}
		findIt()
	}

	def receive: Receive = {
		case Terminated(ref) => if(ref == producer){
			producer = context.system.deadLetters
			log.info(s"Slot producer $ref terminated")
			findSlotProducer()
		}

		case CalculateSlots(slots: Seq[StiltSlot]) =>
			val newWork = slots.take(nCores - work.size)
			work ++= newWork
			newWork foreach calculateSlot
			sender() ! myStatus

		case WorkMaster.Stop =>
			log.info(s"WorkMaster terminated (was $self)")
			context stop self
	}

	private def finishSlot(slot: StiltSlot, msg: Any): Unit = {
		producer ! msg
		work -= slot
		producer ! myStatus
	}

	private def myStatus = WorkMasterStatus(nCores, work.toSeq)

	private def calculateSlot(slot: StiltSlot): Unit = {

		import scala.concurrent.ExecutionContext.Implicits.global

		def calculateOnce(): Future[Unit] = Future{
			log.debug(s"Starting stilt calculation of $slot")

			val stiltOutput = RunStilt.cmd_run(slot)

			log.debug(s"Stilt simulation finished $slot ($stiltOutput)")

			val d = Paths.get(stiltOutput)
			assert(Files.isDirectory(d))

			finishSlot(slot, SlotCalculated(StiltResult(slot, d.resolve("output"))))

			Util.deleteDirRecursively(d)
			log.debug(s"Removed the ${d} directory")
			log.debug(s"Slot calculation for $slot was a success")
		}

		calculateOnce().recoverWith{ case _: Throwable => calculateOnce()}
			.failed
			.foreach{err =>
				log.info(s"Slot calculation for $slot was a failure: " + err.getMessage)
				log.debug(err.getStackTrace().mkString("", "\n", "\n"))
				finishSlot(slot, StiltFailure(slot))
			}
	}
}

object WorkMaster{
	def props(nCores: Int, producerAddress: String) = Props.create(classOf[WorkMaster], Int.box(nCores), producerAddress)

	case object Stop
}
