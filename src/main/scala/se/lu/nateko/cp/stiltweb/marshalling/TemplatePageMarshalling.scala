package se.lu.nateko.cp.stiltweb.marshalling

import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.marshalling.Marshalling._
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshaller
import org.apache.pekko.http.scaladsl.model._
import scala.concurrent.Future
import play.twirl.api.Html

object TemplatePageMarshalling {

	private def getHtml(html: Html, charset: HttpCharset) = HttpResponse(
		entity = HttpEntity(
			ContentType.WithCharset(MediaTypes.`text/html`, charset),
			html.body
		)
	)

	def marshaller: ToResponseMarshaller[Html] = Marshaller(
		_ => html => Future.successful(
			WithOpenCharset(MediaTypes.`text/html`, getHtml(html, _)) :: Nil
		)
	)

}