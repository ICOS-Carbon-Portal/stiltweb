package se.lu.nateko.cp.stiltweb

import akka.http.javadsl.server.CustomRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.*
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.cpauth.core.UserId
import scala.util.Failure
import scala.util.Success

class AuthRouting(authConfig: PublicAuthConfig):

	private val authenticator = Authenticator(authConfig).get

	val user: Directive1[UserId] = cookie(authConfig.authCookieName).flatMap: cookie =>
		CookieToToken.recoverToken(cookie.value).flatMap(authenticator.unwrapToken) match
			case Success(token) =>
				provide(token.userId)
			case Failure(err) =>
				reject(new CpauthAuthenticationFailedRejection(toMessage(err)))

	val userReq: Directive1[UserId] = user | complete(StatusCodes.Unauthorized -> "Please log in with Carbon Portal")

	val userOpt: Directive1[Option[UserId]] = user.map(Some(_)) | provide(None)

	private def toMessage(err: Throwable): String =
		val msg = err.getMessage
		if(msg == null || msg.isEmpty) err.getClass.getName else msg

end AuthRouting

class CpauthAuthenticationFailedRejection(val msg: String) extends CustomRejection
