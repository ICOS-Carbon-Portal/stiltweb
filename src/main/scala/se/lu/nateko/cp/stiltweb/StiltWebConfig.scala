package se.lu.nateko.cp.stiltweb

import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.stiltcluster.ConfigLoader
import spray.json._
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

case class NetCdfConfig(
	dateVars: Seq[String],
	latitudeVars: Seq[String],
	longitudeVars: Seq[String],
	elevationVars: Seq[String]
)

case class StiltWebConfig(
	admins: Seq[String],
	auth: PublicAuthConfig,
	metDataDirectory: String,
	stateDirectory: String,
	netcdf: NetCdfConfig,
	slotStepInMinutes: Int
)

object ConfigReader extends DefaultJsonProtocol{

	implicit val pubAuthConfigFormat: JsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)
	implicit val netcdfConfigFormat: JsonFormat[NetCdfConfig] = jsonFormat4(NetCdfConfig.apply)
	implicit val cpdataConfigFormat: JsonFormat[StiltWebConfig] = jsonFormat6(StiltWebConfig.apply)

	lazy val default: StiltWebConfig = fromAppConfig(ConfigLoader.localWithDefault())

	def fromAppConfig(applicationConfig: Config): StiltWebConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val confJson: String = applicationConfig.getValue("stiltweb").render(renderOpts)

		confJson.parseJson.convertTo[StiltWebConfig]
	}
}
