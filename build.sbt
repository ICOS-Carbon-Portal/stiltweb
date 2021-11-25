ThisBuild / scalaVersion := "2.13.7"

Global / cancelable := true

lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalacOptions ++= Seq(
		"-target:jvm-1.11",
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation",
		"-Wdead-code",
		"-Wnumeric-widen"
	)
)

val akkaVersion = "2.6.17"
val akkaHttpVersion = "10.2.7"

lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.enablePlugins(IcosCpSbtDeployPlugin)
	.settings(
		name := "stiltcluster",
		version := "0.3.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-remote"         % akkaVersion,
			"com.typesafe.akka" %% "akka-slf4j"          % akkaVersion,
			"io.netty"           % "netty"               % "3.10.6.Final",
			"ch.qos.logback"     % "logback-classic"     % "1.1.3",
			"org.scalatest"     %% "scalatest"           % "3.1.0" % "test"
		),

		cpDeployTarget := "stiltcluster",
		cpDeployPlaybook := "stilt.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltcluster",

		run / fork := true,
		run / connectInput := true,

		(reStart / baseDirectory)  := {
			(reStart / baseDirectory).value.getParentFile
		}
	)

val npmPublish = taskKey[Unit]("runs 'npm publish'")
npmPublish := scala.sys.process.Process(Seq("bash", "-c", "npm install && npm run publish")).!

lazy val stiltweb = (project in file("."))
	.dependsOn(stiltcluster)
	.enablePlugins(SbtTwirl,IcosCpSbtDeployPlugin)
	.settings(commonSettings: _*)
	.settings(
		name := "stiltweb",
		version := "0.3.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"  %% "akka-http-spray-json"               % akkaHttpVersion,
			"com.typesafe.akka"  %% "akka-stream"                        % akkaVersion,
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.6.5",
			"se.lu.nateko.cp"    %% "views-core"                         % "0.4.8",
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.1.4",
			"edu.ucar"            % "netcdf4"                            % "4.6.11" excludeAll(
				ExclusionRule(organization = "edu.ucar", name = "cdm")
			),
			"com.typesafe.akka"  %% "akka-testkit"                       % akkaVersion        % "test",
			"org.scalatest"      %% "scalatest"                          % "3.1.0"            % "test"
		),

		cpDeployTarget := "stiltweb",
		cpDeployPlaybook := "stilt.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltweb",

		//Could not get SBT to include hidden files; changing filter settings didn't help
		Test / unmanagedResources := {
			val folder = (Test / unmanagedResourceDirectories).value
			folder.flatMap{dir =>
				import scala.collection.JavaConverters._
				java.nio.file.Files.walk(dir.toPath).iterator.asScala.map(_.toFile)
			}
		},

		// Override the "assembly" command so that we always run "npm publish"
		// first - thus generating javascript files - before we package the
		// "fat" jarfile used for deployment.
		assembly := (Def.taskDyn{
			val original = assembly.taskValue
			// Referencing the task's 'value' field will trigger the npm command
			npmPublish.value
			// Then just return the original "assembly command"
			Def.task(original.value)
		}).value

	)
