package se.lu.nateko.cp.stiltcluster

import java.time.LocalDate

import org.scalatest.FunSuite


class RunStiltTest extends FunSuite {
	val job = Job("HTM", 56.10, 13.42, 150,
				  LocalDate.of(2012, 1, 1),
				  LocalDate.of(2012, 1, 2),
				  "user")
	val slot = StiltSlot.ofString("2012x12x01x00x56.10Nx013.42Ex00150")

	test("Check date to slot conversion") {
		assert(RunStilt.date_to_slot(job.start) == "2012010100")
		assert(RunStilt.date_to_slot(job.stop, "21") == "2012010221")
	}

	test("Check building of stilt calcslots command") {
		assert(RunStilt.build_calcslots_cmd(job.start, job.stop) ==
				   "stilt calcslots 2012010100 2012010200")
	}


	test("Run command") {
		assert(RunStilt.run_cmd("echo foobar") == Seq("foobar"))
	}


	ignore("Calculate slots") {
		assert(RunStilt.cmd_calcslots(job.start, job.stop) == Seq(
				   "2012010100", "2012010103",
				   "2012010106", "2012010109",
				   "2012010112", "2012010115",
				   "2012010118", "2012010121",
				   "2012010200"))
	}

	test("Build run command") {
		assert(RunStilt.build_run_cmd(slot) == "stilt run XXX 56.1 13.42 150 2012120100 2012120100")
	}
}
