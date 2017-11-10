package se.lu.nateko.cp.stiltcluster

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging}
import scala.util.{ Failure, Success }
import se.lu.nateko.cp.stiltrun.RunStilt

class SlotCalculator extends Actor with ActorLogging {

	val slotArchive = context.actorSelection("/user/slotarchive")

	def receive = {
		case CalculateSlotList(job) =>
			log.info(s"Asked to calculate slots for job $job")
			val origin = sender ()
			Future {
				log.info("Future starting")
				// A sequence of strings such as 2012071203
				val strings = RunStilt.cmd_calcslots(job.start, job.stop)
				println(s"(println) Slots calculated (all ${strings.length} of them)")
				// Convert these strings into proper slots
				val slots = strings.map ( parseCalcSlotString(job, _) )
				log.info(s"Slots converted (all ${slots.length} of them)")
				origin ! SlotListCalculated(slots)
			} onComplete {
				// FIXME - Would like automatic logging of stacktraces in failed futures.
				case Success(_) => println("Future successfully completed")
				case Failure(t) => { println("An error has occured: " + t); t.printStackTrace }
			}
			log.info("Started slot calculation in separate future")
	}

	def parseCalcSlotString(job: Job, desc: String): StiltSlot = {
		val r = """(\d{4})(\d{2})(\d{2})(\d{2})""".r // 2012071203
		val r(year, month, day, hour) = desc
		new StiltSlot(new StiltTime(year.toInt, month.toInt, day.toInt, hour.toInt),
					  new StiltPosition(job.lat, job.lon, job.alt))
	}

}
