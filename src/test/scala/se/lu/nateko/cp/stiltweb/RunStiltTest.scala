package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate

import org.scalatest.FunSuite


class RunStiltTest extends FunSuite {
	val job = Job("HTM", 56.10, 13.42, 150,
				  LocalDate.of(2012, 1, 1),
				  LocalDate.of(2012, 1, 2),
				  "user")
	val slot = StiltSlot.ofString("2012x12x01x00x56.10Nx013.42Ex00150")

	test("Run command") {
		assert(RunStilt.run_cmd("echo foobar") == Seq("foobar"))
	}


	test("Build run command") {
		assert(RunStilt.build_run_cmd(slot) == "stilt run XXX 56.1 13.42 150 2012120100 2012120100")
	}
}
