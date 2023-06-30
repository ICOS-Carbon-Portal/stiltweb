package se.lu.nateko.cp.stiltweb.zip

import se.lu.nateko.cp.cpauth.core.UserId
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class PackagingThrottler[T](using ExecutionContext):

	private val jobKeys = mutable.Set.empty[String]

	def runFor(user: UserId, stationId: String)(work: => Future[T]): Either[String, Future[T]] =
		throttle(user.email, userErrorMessage):
			throttle(stationId, stationErrorMessage(stationId)):
				Right(work)

	val userErrorMessage = "You are already running a packaging job at the moment. Please wait a minute and try again."
	def stationErrorMessage(stationId: String) =
		s"Someone else is packaging results for station $stationId at the moment. Please wait a minute and try again."

	private def throttle(key: String, msg: String)(work: => Either[String, Future[T]]): Either[String, Future[T]] = synchronized:
		if jobKeys.contains(key) then Left(msg)
		else work.map: fut =>
			jobKeys.add(key)
			fut.transform: res =>
				synchronized:
					jobKeys.remove(key)
				res

end PackagingThrottler
