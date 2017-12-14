package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory


object ConfigLoader {

	def clusterBase(): Config = ConfigFactory
		.parseResources("clusterbase.conf")
		.withFallback(ConfigFactory.load())
		.resolve()
}
