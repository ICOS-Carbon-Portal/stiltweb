package se.lu.nateko.cp.stiltweb

import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import spray.json._
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory

case class StiltWebConfig(auth: PublicAuthConfig, dummyProperty: String)

object ConfigReader extends DefaultJsonProtocol{

	implicit val pubAuthConfigFormat = jsonFormat2(PublicAuthConfig)
	implicit val cpdataConfigFormat = jsonFormat2(StiltWebConfig)

	def getDefault: StiltWebConfig = fromAppConfig(getAppConfig)

	def getAppConfig: Config = {
		val default = ConfigFactory.load
		val confFile = new java.io.File("application.conf").getAbsoluteFile
		if(!confFile.exists) default
		else ConfigFactory.parseFile(confFile).withFallback(default)
	}

	def fromAppConfig(applicationConfig: Config): StiltWebConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val confJson: String = applicationConfig.getValue("stiltweb").render(renderOpts)
		
		confJson.parseJson.convertTo[StiltWebConfig]
	}
}