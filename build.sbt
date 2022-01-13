ThisBuild / scalaVersion := "3.1.0"

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

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % "test" exclude("org.scala-lang.modules", "scala-xml_3")

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
		cpDeployPlaybook := "stilt.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltcluster",

		run / fork := true,
		run / connectInput := true,

		(reStart / baseDirectory)  := {
			(reStart / baseDirectory).value.getParentFile
		}
	)

val npmPublish = taskKey[Int]("runs 'npm run publish'")
npmPublish := {
	import scala.sys.process.Process
	(Process("npm ci") #&& Process("npm run publish")).!
}

lazy val stiltweb = (project in file("."))
	.dependsOn(stiltcluster)
	.enablePlugins(SbtTwirl,IcosCpSbtDeployPlugin)
	.settings(commonSettings: _*)
	.settings(
		name := "stiltweb",
		version := "0.4.1",

		libraryDependencies := {
			libraryDependencies.value.map{
				case m if m.name.startsWith("twirl-api") => 
					m.cross(CrossVersion.for3Use2_13)
				case m => m
			}
		},

		libraryDependencies ++= Seq(
			"com.typesafe.akka"  %% "akka-http-spray-json"               % akkaHttpVersion cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"  %% "akka-stream"                        % akkaVersion cross CrossVersion.for3Use2_13,
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.6.5" cross CrossVersion.for3Use2_13,
			"se.lu.nateko.cp"    %% "views-core"                         % "0.4.8" cross CrossVersion.for3Use2_13,
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.1.4" cross CrossVersion.for3Use2_13,
			"edu.ucar"            % "netcdf4"                            % "4.6.11" excludeAll(
				ExclusionRule(organization = "edu.ucar", name = "cdm")
			),
			"com.typesafe.akka"  %% "akka-testkit"                       % akkaVersion % "test" cross CrossVersion.for3Use2_13,
			scalaTest
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
			if(npmPublish.value != 0) throw new IllegalStateException("Front end build error")
			// Then just return the original "assembly command"
			Def.task(original.value)
		}).value

	)
