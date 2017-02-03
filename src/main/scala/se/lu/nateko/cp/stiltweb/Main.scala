package se.lu.nateko.cp.stiltweb

import scala.collection.JavaConversions
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.stream.ActorMaterializer
import se.lu.nateko.cp.stiltcluster.SeedRunner

object Main extends App {

	val cluster = new SeedRunner

	implicit val system = ActorSystem("stiltweb")
	implicit val materializer = ActorMaterializer(namePrefix = Some("stiltweb_mat"))
	implicit val dispatcher = system.dispatcher

	val config = ConfigReader.getDefault

	val exceptionHandler = ExceptionHandler{
		case ex =>
			val exMsg = ex.getMessage
			val msg = if(exMsg == null || exMsg.isEmpty) ex.getClass.getName else exMsg
			complete((StatusCodes.InternalServerError, msg))
	}

	val route = new MainRoute(config).route

	Http()
		.bindAndHandle(route, "127.0.0.1", 9010)
		.onSuccess{
			case binding =>
				sys.addShutdownHook{
					val doneFuture = binding.unbind().flatMap{
						_ => system.terminate().zip(cluster.system.terminate())
					}
					Await.result(doneFuture, 3 seconds)
				}
				println(binding)
		}

}
