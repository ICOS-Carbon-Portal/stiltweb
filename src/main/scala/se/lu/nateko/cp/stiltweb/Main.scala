package se.lu.nateko.cp.stiltweb

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable.apply
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.ExceptionHandler
import scala.util.{ Failure, Success }
import se.lu.nateko.cp.stiltcluster.StiltClusterApi
import scala.concurrent.ExecutionContext

@main def main(): Unit =

	val cluster = new StiltClusterApi

	implicit val system: ActorSystem = ActorSystem("stiltweb")
	system.log
	implicit val dispatcher: ExecutionContext = system.dispatcher

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

	Http().newServerAt("127.0.0.1", 9010)
		.bindFlow(route)
		.onComplete{
			case Failure(error)   => error.printStackTrace()
			case Success(binding) =>
				sys.addShutdownHook{
					val ctxt = scala.concurrent.ExecutionContext.Implicits.global
					val doneFuture = binding.unbind()
						.flatMap{
							_ => system.terminate()
						}(using ctxt)
						.flatMap{
							_ => cluster.terminate()
						}(using ctxt)
					Await.result(doneFuture, 3.seconds)
				}
				println(binding)
		}
