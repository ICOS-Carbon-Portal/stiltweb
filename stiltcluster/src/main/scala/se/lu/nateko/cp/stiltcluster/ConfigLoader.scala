package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

object ConfigLoader {

	def load(extraResource: Option[String] = None): Config = ConfigFactory
		.parseFile(new File("application.conf"))
		.withFallback(extraResource match {
			case Some(extra) => ConfigFactory.parseResources(extra)
			case None => ConfigFactory.empty()
		})
		.withFallback(ConfigFactory.parseResources("stiltcluster.conf"))
		.withFallback(ConfigFactory.load())
		.resolve()
}