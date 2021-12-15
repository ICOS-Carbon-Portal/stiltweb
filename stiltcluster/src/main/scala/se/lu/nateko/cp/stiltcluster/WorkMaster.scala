package se.lu.nateko.cp.stiltcluster

import java.nio.file.{ Files, Paths }

import scala.collection.mutable.Set
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Success

import akka.actor.{ Actor, ActorRef, Props, Terminated }
import akka.actor.ActorLogging
import akka.util.Timeout
import java.nio.file.Path


class WorkMaster(nCores: Int, receptionistAddr: String) extends Actor with ActorLogging {
	import WorkMaster._

	var receptionist: ActorRef = context.system.deadLetters
	val work = Set.empty[StiltSlot]

	log.info("WorkMaster starting up")

	override def preStart(): Unit = findReceptionist()

	def findReceptionist(): Unit = {
		implicit val timeout: Timeout = Timeout(3.seconds)
		import context.dispatcher

		def findIt(): Unit = context.actorSelection(receptionistAddr).resolveOne().onComplete{
			case Success(ref) =>
				context.watch(ref)
				log.info(s"Found a receptionist at ${ref.path}")
				receptionist = ref
				ref ! myStatus()
			case _ =>
				context.system.scheduler.scheduleOnce(1.second)(findIt())
		}
		findIt()
	}

	def receive: Receive = {
		case Terminated(ref) => if(ref == receptionist){
			receptionist = context.system.deadLetters
			log.info(s"Receptionist ${ref.path} terminated")
			findReceptionist()
		}

		case CalculateSlots(id, slots: Seq[StiltSlot]) =>
			val newSlots = slots.distinct.filterNot(work.contains)
			log.debug("Received new slots: " + newSlots.mkString(", "))

			val freeCores = nCores - work.size

			if(newSlots.size > freeCores){
				log.warning(s"Received ${newSlots.size} new distinct slots while have only $freeCores free CPU cores")
				if(newSlots.size < 10) {
					log.warning("New slots:")
					newSlots.foreach(slot => log.warning(slot.toString))
				}
			}

			val newWork = newSlots.take(freeCores)
			work ++= newWork
			newWork foreach calculateSlot
			sender() ! myStatus(Some(id))

		case Stop =>
			log.info(s"WorkMaster terminated (was $self)")
			context stop self
	}

	private def myStatus(asResponseTo: Option[Long] = None) = WorkMasterStatus(nCores, work.toSeq, asResponseTo)

	private def calculateSlot(slot: StiltSlot): Unit = {

		import scala.concurrent.ExecutionContext.Implicits.global

		def runStiltGetBaseFolder(prevAttempts: Int): Future[(Int, Path)] = {
			val attempts = prevAttempts + 1

			val res = Future{
				log.debug(s"Attempt $attempts (of max $MaxNumOfAttempts) of stilt calculation of $slot")

				val stiltOutput = RunStilt.cmd_run(slot)

				log.debug(s"Stilt simulation finished $slot ($stiltOutput)")

				val d = Paths.get(stiltOutput)
				assert(Files.isDirectory(d))
				attempts -> d
			}
			if(attempts >= MaxNumOfAttempts)
				res
			else
				res.recoverWith{case _: Throwable => runStiltGetBaseFolder(attempts)}
		}

		def getResult(prevAttempts: Int): Future[StiltOutcome] = runStiltGetBaseFolder(prevAttempts)
			.flatMap{case (attempts, d) =>

				Future{
					val res = SlotCalculated(StiltResult(slot, d.resolve("output")))
					log.debug(s"Slot calculation for $slot was a success")
					res
				}.recoverWith{
					case err: Throwable =>
						logError(err)
						if(attempts < MaxNumOfAttempts) getResult(attempts)
						else Future.successful(
							getFailureInfo(slot, d, err.getMessage)
						)
				}.andThen{
					case _ =>
						Util.deleteDirRecursively(d)
						log.debug(s"Removed the ${d} directory")
				}
			}
			.recover{
				case err: Throwable =>
					logError(err)
					StiltFailure(slot, Seq(err.getMessage), None)
			}

		def logError(err: Throwable): Unit = {
			val errMsg = err.getMessage
			log.info(s"Slot calculation for $slot was a failure: $errMsg")
			log.debug(err.getStackTrace().mkString("", "\n", "\n"))
		}

		getResult(0).foreach{outcome =>
			receptionist ! outcome
			work -= slot
			receptionist ! myStatus()
		}
	}
}

object WorkMaster{

	val MaxNumOfAttempts = 2

	def props(nCores: Int, receptionistAddress: String) = Props.create(classOf[WorkMaster], Int.box(nCores), receptionistAddress)

	case object Stop

	def getFailureInfo(slot: StiltSlot, stiltBaseDir: Path, fallbackErrMsg: String): StiltFailure = {
		val logsDir = stiltBaseDir.resolve("logs")
		val logsZip = Util.zipFolder(logsDir, "PARTICLE.DAT")
		val logErrMsgs = Util.iterateChildren(logsDir)
			.filter(d => Files.isRegularFile(d) && d.getFileName.toString.endsWith(".log"))
			.flatMap(d => Source.fromFile(d.toFile).getLines())
			.filter(_.contains("ERROR"))
			.toIndexedSeq
			.sorted

		val errMsgs = if(logErrMsgs.isEmpty) Seq(fallbackErrMsg) else logErrMsgs

		StiltFailure(slot, errMsgs, Some(logsZip))
	}
}
