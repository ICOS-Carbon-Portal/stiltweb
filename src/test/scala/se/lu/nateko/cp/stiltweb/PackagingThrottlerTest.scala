package se.lu.nateko.cp.stiltweb

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.GivenWhenThen
import se.lu.nateko.cp.stiltweb.zip.PackagingThrottler
import scala.concurrent.ExecutionContext.Implicits.global
import se.lu.nateko.cp.cpauth.core.UserId
import scala.concurrent.Promise
import scala.util.Success
import scala.concurrent.Future


object PackagingThrottlerTests:
	val john = UserId("john")
	val bob = UserId("bob")

import PackagingThrottlerTests.*


class PackagingThrottlerTests extends AnyFunSuite with GivenWhenThen:

	test("Situation when no jobs are being run"):
		Given("a freshly created PackagingThrottler")
		val th = new PackagingThrottler[Int]

		When("a new job is requested to be run")
		val promise = Promise[Int]()
		val job = th.runFor(john, "NNN")(promise.future)

		Then("the job is accepted")
		assert(job.isRight)

	test("Situation of a job already being run by this user"):
		Given("PackagingThrottler has already accepted a job from the user, and the job is still running")
		val th = new PackagingThrottler[Int]
		val promise = Promise[Int]()
		val job1 = th.runFor(john, "AAA")(promise.future)

		When("the user requests another job with the same station id")
		val job2 = th.runFor(john, "AAA")(Future.successful(42))

		Then("the request is denied with an adequate error message")
		assert(job2.left.exists(_ == th.userErrorMessage))

		When("the user requests another job with another station id")
		val job3 = th.runFor(john, "BBB")(Future.successful(42))

		Then("the request is denied with an adequate error message")
		assert(job3.left.exists(_ == th.userErrorMessage))

	test("Situation of a job already being run for a station (by any user)"):
		Given("PackagingThrottler has already accepted a job for the station, and the job is still running")
		val th = new PackagingThrottler[Int]
		val promise = Promise[Int]()
		val job1 = th.runFor(john, "AAA")(promise.future)

		When("another user requests another job with the same station id")
		val job2 = th.runFor(bob, "AAA")(Future.successful(33))

		Then("the request is denied with an adequate error message")
		assert(job2.left.exists(_ == th.stationErrorMessage("AAA")))

end PackagingThrottlerTests


class PackagingThrottlerAsyncTests extends AsyncFunSuite with GivenWhenThen:

	test("Situation of a job previously having been run by this user"):

		Given("PackagingThrottler has accepted a job from a user")
		val th = new PackagingThrottler[Int]
		val promise = Promise[Int]()
		val job1 = th.runFor(john, "AAA")(promise.future)

		When("the job finishes, and the user requests another job for the same station")
		promise.success(1)
		val runningJob = job1.getOrElse(fail("job 1 was not accepted"))
		runningJob.map: _ =>
			val job2 = th.runFor(john, "AAA")(Future.successful(0))
			Then("the second job is accepted for execution")
			assert(job2.isRight)
