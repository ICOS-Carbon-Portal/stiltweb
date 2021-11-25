package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

import se.lu.nateko.cp.stiltcluster.Job

import spray.json._
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport._
import org.scalatest.funsuite.AnyFunSuite

class JsonTest extends AnyFunSuite {

	// spray-json is way up in the cloud, this test brings it down to the ground
	// so that one can get a feel for what's happening.
	test("Serialize and deserialize JSON") {

		val job = Job("HTM", 56.10, 13.42, 150,
					  LocalDate.of(2012, 1, 1),
					  LocalDate.of(2012, 1, 2),
					  "user")

		// This is the string the toJson returned for me.
		//   {"stop":"2012-01-02","id":"job_1438388676","alt":150,"lon":13.42,
		//    "siteId":"HTM","userId":"user","start":"2012-01-01","lat":56.1}
		// Since the order of the keys can vary, we convert it to lines and sort
		// them before testing.
		val j = job.toJson
		val s = j.toString()
		val t = s.slice(1, s.length-1).split(",").map(_.trim).sorted.mkString("\n")

		// This particular test checks that the timeStarted and timeStopped
		// fields are not being serialized.
		assert(t == """"alt":150
					   |"id":"job_940257578"
					   |"lat":56.1
					   |"lon":13.42
					   |"siteId":"HTM"
					   |"start":"2012-01-01"
					   |"stop":"2012-01-02"
					   |"userId":"user"""".stripMargin)

		val job2 = j.convertTo[Job]
		assert(job == job2)
	}
}
