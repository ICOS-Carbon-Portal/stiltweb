package se.lu.nateko.cp.stiltweb

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.stream.ActorMaterializer
import scala.util.{ Failure, Success }
import se.lu.nateko.cp.stiltcluster.StiltClusterApi

object Main extends App {

	val cluster = new StiltClusterApi

	implicit val system = ActorSystem("stiltweb")
	system.log
	implicit val materializer = ActorMaterializer(namePrefix = Some("stiltweb_mat"))
	implicit val dispatcher = system.dispatcher

	val config = ConfigReader.default

	val exceptionHandler = ExceptionHandler{
		case ex =>
			val exMsg = ex.getMessage
			val msg = if(exMsg == null || exMsg.isEmpty) ex.getClass.getName else exMsg
			val stack = ex.getStackTrace.map(_.toString).mkString("\n", "\n", "")
			complete((StatusCodes.InternalServerError, msg + stack))
	}

	val route = {
		val inner = new MainRoute(config, cluster).route
		handleExceptions(exceptionHandler){inner}
	}

	Http()
		.bindAndHandle(route, "127.0.0.1", 9010)
		.onComplete{
			case Failure(error)   => error.printStackTrace()
			case Success(binding) =>
				sys.addShutdownHook{
					val ctxt = scala.concurrent.ExecutionContext.Implicits.global
					val doneFuture = binding.unbind().flatMap{
						_ => system.terminate()
					}(ctxt)
					Await.result(doneFuture, 3 seconds)
				}
				println(binding)
		}

}
