package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.{ File }

case class StiltEnv(debugRun: Option[String],
					debugLog: Option[String],
					mainDirectory: File,
					launchScript: String,
					containerName: String,
					logSizeLimit: Int,
					stateDir: File) {

	if (! stateDir.isDirectory())
		throw new Exception(s"'stateDir' must be a directory, but '${stateDir}' isn't")

	if (! stateDir.canWrite())
		throw new Exception(s"Must have write permissions for ${stateDir} ('stateDir')")

}



object ConfigLoader {

	def load(extraResource: Option[String] = None): Config = ConfigFactory
		.parseFile(new File("local.conf"))
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
			logSizeLimit = conf.getInt("logSizeLimit"),
			stateDir = new File(conf.getString("stateDirectory"))
		)
	}
}
