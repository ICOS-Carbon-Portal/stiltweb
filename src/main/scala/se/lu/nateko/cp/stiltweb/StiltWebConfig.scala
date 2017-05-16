package se.lu.nateko.cp.stiltweb

import java.io.File
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import spray.json._
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory

case class NetCdfConfig(
	dateVars: Seq[String],
	latitudeVars: Seq[String],
	longitudeVars: Seq[String],
	elevationVars: Seq[String]
)

case class StiltWebConfig(
	admins: Seq[String],
	auth: PublicAuthConfig,
	mainDirectory: String,
	jobsOutputDirectory: String,
	metDataDirectory: String,
	netcdf: NetCdfConfig,
	stations: Seq[Seq[String]]
)

object ConfigReader extends DefaultJsonProtocol{

	implicit val pubAuthConfigFormat = jsonFormat2(PublicAuthConfig)
	implicit val netcdfConfigFormat = jsonFormat4(NetCdfConfig)
	implicit val cpdataConfigFormat = jsonFormat7(StiltWebConfig)

	def getDefault: StiltWebConfig = fromAppConfig(getAppConfig)

	def getAppConfig: Config = {
		// First load configuration from local.conf, located in the current
		// directory, then fall back to application.conf, referenc.conf (all
		// found in classpath) etc etc
		ConfigFactory.parseFile(new File("local.conf")).withFallback(ConfigFactory.load())
	}

	def fromAppConfig(applicationConfig: Config): StiltWebConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val confJson: String = applicationConfig.getValue("stiltweb").render(renderOpts)

		confJson.parseJson.convertTo[StiltWebConfig]
	}
}
