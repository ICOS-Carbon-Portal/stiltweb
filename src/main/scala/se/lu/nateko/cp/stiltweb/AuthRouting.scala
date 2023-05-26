package se.lu.nateko.cp.stiltweb


import akka.http.javadsl.server.CustomRejection
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.AuthToken
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.cpauth.core.UserId

import scala.util.Failure
import scala.util.Success

class AuthRouting(authConfig: PublicAuthConfig):

	private[this] val authenticator = Authenticator(authConfig).get

	val authToken: Directive1[AuthToken] = cookie(authConfig.authCookieName).flatMap{cookie =>
		CookieToToken.recoverToken(cookie.value).flatMap(authenticator.unwrapToken) match
			case Success(token) =>
				provide(token)
			case Failure(err) =>
				reject(CpauthAuthFailedRejection("Authentication cookie invalid or absent: " + toMessage(err)))
	}

	val user: Directive1[UserId] = authToken.flatMap{token =>
		if token.source != AuthSource.AtmoAccess then
			reject(CpauthAuthFailedRejection("User did not log in through ATMO ACCESS"))
		else provide(token.userId)
	}

	private def toMessage(err: Throwable): String =
		val msg = err.getMessage
		if(msg == null || msg.isEmpty) err.getClass.getName else msg

end AuthRouting

class CpauthAuthFailedRejection(val msg: String) extends CustomRejection
