package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

import org.scalatest._
import se.lu.nateko.cp.stiltcluster._


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
