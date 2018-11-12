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


class JobMonitorTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender with BeforeAndAfterAll{

	val tmp = Files.createTempDirectory("jobmonitor")

	override def afterAll() = {
		system.terminate()
		Util.deleteDirRecursively(tmp)
	}

	test("send/receive") {
		val job = Job("XXX", 46.55, 7.98, 720,
					  LocalDate.of(2012, 12, 8),
					  LocalDate.of(2012, 12, 8), "nisse")

		val slotStep = 180

		val dir = new JobDir(job, tmp)

		val slots = JobMonitor.calculateSlots(job, slotStep)

		val sProd = TestProbe()
		system.actorOf(Props(new ForwardActor(sProd.ref)), "slotproducer")

		val sDmak = TestProbe()
		system.actorOf(Props(new ForwardActor(sDmak.ref)), "dashboardmaker")

		system.actorOf(JobMonitor.props(dir, slotStep), name="jobmonitor")

		sProd.expectMsgPF() {
			case (RequestManySlots(reqs)) => assert(reqs === slots)
		}
		sDmak.expectMsgPF() {
			case(JobInfo(_, tot, rem)) => {
				assert(tot == slots.size);
				assert(rem == 0) }
		}
	}
}

class JobMonitorCalculateSlotsTests extends FunSuite{

	test("two days have 16 3-hour slots"){
		val pos = StiltPosition(50.0, 10.0, 100)
		val job = Job("station", pos.lat, pos.lon, pos.alt, LocalDate.of(2012, 12, 7), LocalDate.of(2012, 12, 8), "username")
		val slots = JobMonitor.calculateSlots(job, 180)

		assert(slots.size === 16)
		val s0 = slots.head
		assert(s0.pos === pos)
		assert(s0.time === StiltTime(2012, 12, 7, 0))
		assert(slots.last.time === StiltTime(2012, 12, 8, 21))
	}
}
