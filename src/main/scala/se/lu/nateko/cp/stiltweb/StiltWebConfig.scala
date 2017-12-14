package se.lu.nateko.cp.stiltweb

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
	stateDirectory: String,
	netcdf: NetCdfConfig,
	stations: Seq[Seq[String]]
)

object ConfigReader extends DefaultJsonProtocol{

	implicit val pubAuthConfigFormat = jsonFormat2(PublicAuthConfig)
	implicit val netcdfConfigFormat = jsonFormat4(NetCdfConfig)
	implicit val cpdataConfigFormat = jsonFormat8(StiltWebConfig)

	lazy val default: StiltWebConfig = fromAppConfig(
		ConfigFactory.parseFile(new java.io.File("local.conf"))
			.withFallback(ConfigFactory.load())
	)

	def fromAppConfig(applicationConfig: Config): StiltWebConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val confJson: String = applicationConfig.getValue("stiltweb").render(renderOpts)

		confJson.parseJson.convertTo[StiltWebConfig]
	}
}
