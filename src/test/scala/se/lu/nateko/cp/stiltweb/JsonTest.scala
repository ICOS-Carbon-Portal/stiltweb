package se.lu.nateko.cp.stiltweb

import java.time.LocalDate

import se.lu.nateko.cp.stiltcluster.Job

import spray.json.*
import se.lu.nateko.cp.stiltweb.marshalling.StiltJsonSupport.{given RootJsonFormat[Job]}
import org.scalatest.funsuite.AnyFunSuite

class JsonTest extends AnyFunSuite:
	test("Serialize and deserialize JSON") {

		val job = Job("HTM", 56.10, 13.42, 150,
					  LocalDate.of(2012, 1, 1),
					  LocalDate.of(2012, 1, 2),
					  "user").submittedNow

		val job2 = job.toJson.compactPrint.parseJson.convertTo[Job]
		assert(job == job2)
	}
