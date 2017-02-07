package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

case class StiltEnv(mainFolder: File, launchScript: String, containerName: String, logSizeLimit: Int)

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
		val conf = ConfigFactory.parseResources("stiltenv.conf").getConfig("stiltenv")
		StiltEnv(
			mainFolder = new File(conf.getString("mainFolder")),
			launchScript = conf.getString("launchScript"),
			containerName = conf.getString("containerName"),
			logSizeLimit = conf.getInt("logSizeLimit")
		)
	}
}