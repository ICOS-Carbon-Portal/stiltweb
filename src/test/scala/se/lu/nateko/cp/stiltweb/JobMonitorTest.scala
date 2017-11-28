package se.lu.nateko.cp.stiltweb

import akka.actor.{ Actor, ActorRef }
import akka.testkit.TestProbe
import java.nio.file.Files
import java.time.LocalDate

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import se.lu.nateko.cp.stiltcluster._


// Sad panda.
// https://stackoverflow.com/a/21285315
class ForwardActor(to: ActorRef) extends Actor {
	def receive = {
		case x => to.forward(x)
	}
}


class JobMonitorTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender {

	def afterAll = system.terminate()

	test("send/receive") {
		val tmp = Files.createTempDirectory("jobmonitor")
		val job = Job("XXX", 46.55, 7.98, 720,
					  LocalDate.of(2012, 12, 8),
					  LocalDate.of(2012, 12, 8), "nisse")

		val dir = new JobDir(job, tmp)

		val sCalc = TestProbe()
		system.actorOf(Props(new ForwardActor(sCalc.ref)), "slotcalculator")

		val sProd = TestProbe()
		system.actorOf(Props(new ForwardActor(sProd.ref)), "slotproducer")

		val jobM = system.actorOf(Props(new JobMonitor(dir)), name="jobmonitor")

		sCalc.expectMsgPF() {
			case (CalculateSlotList(_)) => ()
		}

		val slots = Seq("00", "03", "06", "09", "12", "15", "18", "21").map { t =>
			StiltSlot.ofString(s"2012x12x08x${t}x46.55Nx007.98Ex00720")
		}

		jobM ! SlotListCalculated(slots)

		sProd.expectMsgPF() {
			case (RequestManySlots(reqs)) => assert(reqs == slots)
		}
	}
}
