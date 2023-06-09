package se.lu.nateko.cp.stiltweb

import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.stiltcluster.ConfigLoader
import spray.json._
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import se.lu.nateko.cp.data.formats.netcdf.NetCdfViewServiceConfig

case class NetCdfConfig(
	dateVars: Seq[String],
	latitudeVars: Seq[String],
	longitudeVars: Seq[String],
	elevationVars: Seq[String]
) extends NetCdfViewServiceConfig

case class AtmoAccessConfig(
	openidEndpointUrl: String,
	vaApiUrl: String,
	clientId: String,
	ssoClientId: String,
	userName: String,
	userPassword: String,
	baseStiltUrl: String,
)

case class StiltWebConfig(
	admins: Seq[String],
	auth: PublicAuthConfig,
	metDataDirectory: String,
	stateDirectory: String,
	netcdf: NetCdfConfig,
	atmoAccess: AtmoAccessConfig,
	slotStepInMinutes: Int
)

object ConfigReader extends DefaultJsonProtocol{

	given JsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)
	given JsonFormat[NetCdfConfig] = jsonFormat4(NetCdfConfig.apply)
	given JsonFormat[AtmoAccessConfig] = jsonFormat7(AtmoAccessConfig.apply)
	given JsonFormat[StiltWebConfig] = jsonFormat7(StiltWebConfig.apply)

	lazy val default: StiltWebConfig = fromAppConfig(ConfigLoader.localWithDefault())

	def fromAppConfig(applicationConfig: Config): StiltWebConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val confJson: String = applicationConfig.getValue("stiltweb").render(renderOpts)

		confJson.parseJson.convertTo[StiltWebConfig]
	}
}
