package se.lu.nateko.cp.stiltweb.test

import java.time.LocalDate
import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.Job
import se.lu.nateko.cp.stiltweb.RunStilt

class RunStiltTest extends FunSuite {
	val job = Job("", 0, 0, 0,
				  LocalDate.of(2012, 1, 1),
				  LocalDate.of(2012, 1, 2),
				  "user")

//	val path = getClass.getResource("/stilt-test-frontend.sh").getPath

	test("Check date to slot conversion") {
		assert(RunStilt.date_to_slot(job.start) == "2012010100")
		assert(RunStilt.date_to_slot(job.stop, "21") == "2012010221")
	}

	test("Check building of stilt calcslots command") {
		assert(RunStilt.job_to_calcslots_cmd(job) ==
				   "stilt calcslots 2012010100 2012010200")
	}


	test("Run command") {
		assert(RunStilt.run_cmd("echo foobar") == Seq("foobar"))
	}


	test("Calculate slots") {
		assert(RunStilt.cmd_calcslots(job) == Seq("2012010100", "2012010103",
												  "2012010106", "2012010109",
												  "2012010112", "2012010115",
												  "2012010118", "2012010121",
												  "2012010200"))
	}
}
