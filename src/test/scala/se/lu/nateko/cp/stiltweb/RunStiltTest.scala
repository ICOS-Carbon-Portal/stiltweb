package se.lu.nateko.cp.stiltcluster

import org.scalatest.funsuite.AnyFunSuite


class RunStiltTest extends AnyFunSuite:

	test("runCommand"):
		assert(RunStilt.runCommand(Seq("echo", "foobar")) === Seq("foobar"))

	test("buildStiltCommand"):
		val StiltSlot(slot) = "2012x12x01x00x56.10Nx013.42Ex00150" : @unchecked
		val expected = "stilt run XXX 56.10 013.42 150 2012120100 2012120100"
		assert(RunStilt.buildStiltCommand(slot) === expected.split(" ").toIndexedSeq)
