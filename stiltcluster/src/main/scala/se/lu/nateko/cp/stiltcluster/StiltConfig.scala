package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

case class StiltEnv(debugRun: Option[String],
					debugLog: Option[String],
					mainDirectory: File,
					launchScript: String,
					containerName: String,
					logSizeLimit: Int)


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

	def loadStiltEnv : StiltEnv = {
		val conf = ConfigFactory.parseFile(new File("local.conf"))
			.withFallback(ConfigFactory.parseResources("stiltenv.conf"))
			.getConfig("stiltenv")

		val debugRun: Option[String] =
			if (conf.hasPath("debugRun")) Some(conf.getString("debugRun")) else None

		val debugLog: Option[String] =
			if (conf.hasPath("debugLog")) Some(conf.getString("debugLog")) else None

		StiltEnv(
			debugRun = debugRun,
			debugLog = debugLog,
			mainDirectory = new File(conf.getString("mainDirectory")),
			launchScript = conf.getString("launchScript"),
			containerName = conf.getString("containerName"),
			logSizeLimit = conf.getInt("logSizeLimit")
		)
	}
}
