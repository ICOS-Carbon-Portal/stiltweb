package se.lu.nateko.cp.stiltcluster.test

import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.{StiltPosition, StiltTime, StiltSlot}


class StiltTimeTest extends FunSuite {

	test("from/to string") {
		val s = "2012x01x01x03"
		val t = StiltTime.ofString(s)
		assert(t.year == 2012)
		assert(t.month == 1)
		assert(t.day == 1)
		assert(t.hour == 3)

		assert(t.toString == s)
	}
}


class StiltPositionTest extends FunSuite {

	test("from/to string, negative") {
		val s = "20.01Sx150.01Wx01234"
		val p = StiltPosition.ofString(s)
		assert(p.lat == -20.01)
		assert(p.lon == -150.01)
		assert(p.alt == 1234)
	}

	test("from/to string, positive") {
		val s = "20.01Nx050.01Ex01234"
		val p = StiltPosition.ofString(s)
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

		val slt2 = StiltSlot.ofString(expected)
		assert (slt2 == slt)
	}

	test("ofFilename") {
		val s = "foot2012x12x08x18x46.55Nx007.98Ex00720_aggreg.nc"
		val (prefix, slot, suffix) = StiltSlot.ofFilename(s)
		assert(prefix == "foot")
		assert(suffix == "_aggreg.nc")
		assert(slot.year == 2012)
		assert(slot.month == 12)
		assert(slot.day == 8)
		assert(slot.hour == 18)
		assert(slot.lat == 46.55)
		assert(slot.lon == 7.98)
		assert(slot.alt == 720)
	}
}
