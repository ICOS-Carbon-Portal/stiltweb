package se.lu.nateko.cp.stiltcluster.test

import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.{StiltPosition, StiltTime, StiltSlot}

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
}
