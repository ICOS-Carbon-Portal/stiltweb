ThisBuild / scalaVersion := "3.8.2"

Global / cancelable := true

lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalacOptions ++= Seq(
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation",
		//"-Wvalue-discard"
		//"-Wunused:all"
	)
)

val pekkoVersion     = "1.4.0"
val pekkoHttpVersion = "1.3.0"

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % "test"

lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.enablePlugins(IcosCpSbtDeployPlugin)
	.settings(
		name := "stiltcluster",
		version := "0.4.1",
		libraryDependencies ++= Seq(
			"org.apache.pekko" %% "pekko-remote"         % pekkoVersion,
			"org.apache.pekko" %% "pekko-slf4j"          % pekkoVersion,
			"ch.qos.logback"     % "logback-classic"     % "1.1.3",
			scalaTest
		),

		cpDeployTarget := "stiltcluster",
		cpDeployPlaybook := "stiltcluster.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltcluster",
		cpDeployPreAssembly := (Test / test).value,

		run / fork := true,
		run / connectInput := true,

		(reStart / baseDirectory)  := {
			(reStart / baseDirectory).value.getParentFile
		}
	)

val npmPublish = taskKey[Unit]("runs 'npm run publish'")
npmPublish := {
	import scala.sys.process.Process
	val log = streams.value.log
	val exitCode = (Process("npm ci") #&& Process("npm run publish")).!
	if(exitCode == 0) log.info("Front end build successfull")
	else sys.error("Front end build error")
}

lazy val stiltweb = (project in file("."))
	.dependsOn(stiltcluster)
	.enablePlugins(SbtTwirl,IcosCpSbtDeployPlugin)
	.settings(commonSettings: _*)
	.settings(
		name := "stiltweb",
		version := "0.4.3",

		libraryDependencies ++= Seq(
			"org.apache.pekko"   %% "pekko-http-spray-json"              % pekkoHttpVersion,
			"org.apache.pekko"   %% "pekko-stream"                       % pekkoVersion,
			"se.lu.nateko.cp"    %% "views-core"                         % "0.7.10",
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.10.1", //to force newer version
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.3.1" excludeAll("com.google.protobuf"),
			"edu.ucar"            % "netcdf4"                            % "5.5.3" excludeAll("com.google.protobuf"),
			"org.apache.pekko"   %% "pekko-testkit"                      % pekkoVersion % "test",
			scalaTest
		),

		cpDeployTarget := "stiltweb",
		cpDeployPlaybook := "stiltweb.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltweb",
		cpDeployPreAssembly := {
			streams.value.log.warn(s"MAKE SURE THAT SCALA_VERSION variable in gulpfile.js is equal to ${scalaVersion.value}")
			Def.sequential(
				stiltcluster / clean,
				clean,
				Test / test,
				npmPublish
			).value
		},

		//Could not get SBT to include hidden files; changing filter settings didn't help
		Test / unmanagedResources := {
			val folder = (Test / unmanagedResourceDirectories).value
			folder.flatMap{dir =>
				import scala.jdk.CollectionConverters.*
				java.nio.file.Files.walk(dir.toPath).iterator.asScala.map(_.toFile)
			}
		},

	)
