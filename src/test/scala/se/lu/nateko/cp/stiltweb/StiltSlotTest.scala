package se.lu.nateko.cp.stiltweb

import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.{StiltPosition, StiltTime, StiltSlot}


class StiltTimeTest extends FunSuite {

	test("from/to string") {
		val s = "2012x01x01x03"
		val StiltTime(t) = s
		assert(t.year == 2012)
		assert(t.month == 1)
		assert(t.day == 1)
		assert(t.hour == 3)

		assert(t.toString == s)
	}
}


class StiltPositionTest extends FunSuite {

	test("from/to string, negative") {
		val StiltPosition(p) = "20.01Sx150.01Wx01234"
		assert(p.lat == -20.01)
		assert(p.lon == -150.01)
		assert(p.alt == 1234)
	}

	test("from/to string, positive") {
		val StiltPosition(p) = "20.01Nx050.01Ex01234"
		assert(p.lat == 20.01)
		assert(p.lon == 50.01)
		assert(p.alt == 1234)
	}
}


class StiltSlotTest extends FunSuite {

	val tim = StiltTime(2012, 12, 1, 0)
	val pos = StiltPosition(56.10, -13.42, 150)
	val slt = StiltSlot(tim, pos)

	test("StiltSlot to/from string") {
		val expected = "2012x12x01x00x56.10Nx013.42Wx00150"
		assert(slt.toString == expected)

		val StiltSlot(slt2) = expected
		assert (slt2 == slt)
	}

}
