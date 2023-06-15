package se.lu.nateko.cp.stiltweb.zip

import se.lu.nateko.cp.cpauth.core.UserId
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class PackagingThrottler[T](using ExecutionContext):

	private val jobKeys = mutable.Set.empty[String]

	def runFor(user: UserId, stationId: String)(work: => Future[T]): Option[Future[T]] =
		throttle(user.email):
			throttle(stationId):
				Some(work)

	private def throttle(key: String)(work: => Option[Future[T]]): Option[Future[T]] = synchronized:
		if jobKeys.contains(key) then None
		else work.map: fut =>
			jobKeys.add(key)
			fut.onComplete: _ =>
				synchronized:
					jobKeys.remove(key)
			fut

end PackagingThrottler
