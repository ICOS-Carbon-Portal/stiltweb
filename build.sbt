
cancelable in Global := true

lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := "2.11.11",
	scalacOptions ++= Seq(
		"-unchecked",
		"-deprecation",
		"-Xlint",
		"-Ywarn-dead-code",
		"-language:_",
		"-target:jvm-1.8",
		"-encoding", "UTF-8"
	)
)

val akkaVersion = "2.4.16"
val akkaHttpVersion = "10.0.6"


lazy val buildFrontend = taskKey[Unit]("Builds the front end projects")
lazy val frontendThenAssembly = taskKey[File]("Builds the front end projects and then runs assembly")
lazy val deploy = inputKey[Unit]("Deploys to production using Ansible (depends on 'infrastructure' project)")


lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.settings(
		name := "stiltcluster",
		version := "0.1.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"    %% "akka-cluster"     % akkaVersion,
			"com.typesafe.akka"    %% "akka-slf4j"       % akkaVersion
		),
		fork in run := true,
		connectInput in run := true
	)


def runNpmTask(command: String, log: ProcessLogger): Unit = {
	val lines = sbt.Process(command).lines
	lines.foreach(l => log.info(l))
	if(lines.size > 15) sys.error(s"Error running '$command'")
}

lazy val stiltweb = (project in file("."))
	.dependsOn(stiltcluster)
	.enablePlugins(SbtTwirl)
	.settings(commonSettings: _*)
	.settings(
		name := "stiltweb",
		version := "0.1.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"  %% "akka-http-spray-json"               % akkaHttpVersion,
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.5-SNAPSHOT",
			"se.lu.nateko.cp"    %% "views-core"                         % "0.2-SNAPSHOT",
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.1.0-SNAPSHOT",
			"org.scalatest"      %% "scalatest"                          % "3.0.1"            % "test"
		),
		buildFrontend := {
			val log = streams.value.log
			runNpmTask("npm run gulp publishworker", log)
			runNpmTask("npm run gulp publishviewer", log)
		},

		frontendThenAssembly := {
			(assembly in stiltcluster).value
			Def.sequential(buildFrontend, assembly).value
		},

		deploy := {
			val log = streams.value.log

			val args: Seq[String] = sbt.Def.spaceDelimited().parsed

			val check = args.toList match{
				case "to" :: "production" :: Nil =>
					log.info("Performing a REAL deployment to production")
					""
				case _ =>
					log.warn("Performing a TEST deployment, use 'deploy to production' for a real one")
					"--check"
			}
			frontendThenAssembly.value
			val ymlPath = new java.io.File("../infrastructure/devops/stilt.yml").getCanonicalPath
			val inventoryPath = new java.io.File("../infrastructure/devops/production.inventory").getCanonicalPath
			sbt.Process(s"""ansible-playbook $check -i $inventoryPath $ymlPath""").run(true).exitValue()
		}
	)

