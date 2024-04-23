package se.lu.nateko.cp.stiltweb

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.Unmarshal
import se.lu.nateko.cp.cpauth.core.UserId
import spray.json.DefaultJsonProtocol
import spray.json.*

import java.time.Instant
import scala.concurrent.Future
import scala.util.Success

class AtmoAccessClient(conf: AtmoAccessConfig)(using system: ActorSystem):
	import AtmoAccessClient.*
	import system.dispatcher

	private var token: Option[AccessToken] = None
	private val http = Http()

	export conf.baseStiltUrl

	def log(info: AppInfo): Future[Done] =
		val doneFut = for
			token <- getAccessToken()
			entity <- Marshal(info).to[RequestEntity]
			req = HttpRequest(
				method = HttpMethods.POST,
				uri = conf.vaApiUrl,
				headers = List(Authorization(OAuth2BearerToken(token.value))),
				entity = entity
			)
			res <- http.singleRequest(req)
			resTxt <- Unmarshal(res.entity).to[String]
			_ <- if res.status.isSuccess then Future.successful(Done)
				else utils.failure(resTxt)
		yield Done
		doneFut.recover{
			case exc =>
				system.log.warning(s"Failed to log application info to ATMO ACCESS VA API: ${exc.getMessage}")
				Done
		}
	end log

	private def getAccessToken(): Future[AccessToken] = token
		.filter(_.isFresh())
		.fold(
			fetchNewAccessToken().andThen{
				case Success(t) => token = Some(t)
				case _          => token = None
			}
		)(Future.successful)

	private def fetchNewAccessToken(): Future[AccessToken] =
		val req = HttpRequest(
			method = HttpMethods.POST,
			uri = conf.openidEndpointUrl + "token",
			entity = FormData(
				"grant_type" -> "password",
				"client_id"  -> conf.clientId,
				"username"   -> conf.userName,
				"password"   -> conf.userPassword
			).toEntity
		)
		http.singleRequest(req)
			.flatMap(res => Unmarshal(res.entity).to[AccessTokenDto])
			.map(_.makeToken())

end AtmoAccessClient

object AtmoAccessClient extends DefaultJsonProtocol:

	case class AppInfo(
		user: UserId,
		startDate: Instant,
		endDate: Option[Instant],
		resultUrl: String,
		infoUrl: Option[String],
		comment: Option[String],
		service: String = "FOOTPRINTS"
	)

	import se.lu.nateko.cp.cpauth.core.JsonSupport.given

	given RootJsonFormat[AppInfo] = jsonFormat7(AppInfo.apply)

	private class AccessToken(val value: String, val expires: Long):
		def isFresh(): Boolean = System.currentTimeMillis() < (expires - 10000L)

	private case class AccessTokenDto(access_token: String, expires_in: Int):
		def makeToken() = AccessToken(access_token, System.currentTimeMillis() + expires_in * 1000L)

	private given RootJsonFormat[AccessTokenDto] = jsonFormat2(AccessTokenDto.apply)


end AtmoAccessClient
