package se.lu.nateko.cp.stiltweb

import akka.actor.ActorSystem
import AtmoAccessClient.AppInfo
import java.time.Instant
import se.lu.nateko.cp.cpauth.core.UserId

object Playground:

	given system: ActorSystem = ActorSystem("playground")
	val atmo = AtmoAccessClient(ConfigReader.default.atmoAccess)

	def newAppInfo = AppInfo(
		user = UserId("testing.dummy@icos-cp.eu"),
		startDate = Instant.now(),
		endDate = None,
		resultUrl = s"${atmo.baseStiltUrl}/dummyForTesting",
		infoUrl = None,
		comment = Some("This is a test from ICOS' footprints service")
	)
	def stop(): Unit = system.terminate()
