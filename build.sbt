ThisBuild / scalaVersion := "3.2.0"

Global / cancelable := true

lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalacOptions ++= Seq(
		"-Xtarget:11",
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation",
	)
)

val akkaVersion = "2.6.17"
val akkaHttpVersion = "10.2.7"

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % "test"

lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.enablePlugins(IcosCpSbtDeployPlugin)
	.settings(
		name := "stiltcluster",
		version := "0.4.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-remote"         % akkaVersion cross CrossVersion.for3Use2_13,
			"com.typesafe.akka" %% "akka-slf4j"          % akkaVersion cross CrossVersion.for3Use2_13,
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
			"com.typesafe.akka"  %% "akka-http-spray-json"               % akkaHttpVersion excludeAll("io.spray") cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"  %% "akka-stream"                        % akkaVersion cross CrossVersion.for3Use2_13,
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.7.0",
			"se.lu.nateko.cp"    %% "views-core"                         % "0.6.2",
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.3.0",
			"com.typesafe.akka"  %% "akka-testkit"                       % akkaVersion % "test" cross CrossVersion.for3Use2_13,
			scalaTest
		),

		cpDeployTarget := "stiltweb",
		cpDeployPlaybook := "stiltweb.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltweb",
		cpDeployPreAssembly := {
			streams.value.log.warn(s"MAKE SURE THAT SCALA_VERSION variable in gulpfile.js is equal to ${scalaVersion.value}")
			Def.sequential(Test / test, npmPublish).value
		},

		//Could not get SBT to include hidden files; changing filter settings didn't help
		Test / unmanagedResources := {
			val folder = (Test / unmanagedResourceDirectories).value
			folder.flatMap{dir =>
				import scala.collection.JavaConverters._
				java.nio.file.Files.walk(dir.toPath).iterator.asScala.map(_.toFile)
			}
		},

	)
