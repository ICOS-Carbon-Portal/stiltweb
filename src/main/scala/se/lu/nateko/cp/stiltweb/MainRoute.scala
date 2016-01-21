package se.lu.nateko.cp.stiltweb

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

class MainRoute(config: StiltWebConfig) {

	def route: Route = get{
		complete(config.dummyProperty)
	}
}