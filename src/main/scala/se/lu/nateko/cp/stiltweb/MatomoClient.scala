package se.lu.nateko.cp.stiltweb

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.util.{Failure, Success}

class MatomoClient(config: MatomoConfig)(using system: ActorSystem):

	import system.dispatcher

	val baseStiltUrl: String = config.baseStiltUrl

	def trackEvent(
		category: String,
		action: String,
		name: String,
		eventUrl: String,
		userId: String,
		eventTime: Instant,
		clientIp: Option[String] = None
	): Unit =
		val enc = URLEncoder.encode(_: String, StandardCharsets.UTF_8)
		val params = Seq(
			"idsite"  -> config.siteId.toString,
			"rec"     -> "1",
			"e_c"     -> category,
			"e_a"     -> action,
			"e_n"     -> name,
			"url"     -> eventUrl,
			"uid"     -> userId,
			"cdt"     -> eventTime.getEpochSecond.toString
		) ++ (if config.authToken.nonEmpty then Seq("token_auth" -> config.authToken) else Nil)
		  ++ clientIp.map("cip" -> _).toSeq

		val queryString = params.map((k, v) => s"${enc(k)}=${enc(v)}").mkString("&")

		Http().singleRequest(HttpRequest(uri = s"${config.url}?$queryString")).onComplete:
			case Failure(ex) =>
				system.log.warning("Matomo tracking failed for event '{}': {}", action, ex.getMessage)
			case Success(resp) =>
				resp.discardEntityBytes()
				if !resp.status.isSuccess() then
					system.log.warning("Matomo returned {} for event '{}'", resp.status, action)
