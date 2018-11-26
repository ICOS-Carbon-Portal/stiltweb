scalaVersion in ThisBuild := "2.12.7"

cancelable in Global := true

// By default sbt will exclude files starting with a dot when copying
// resources. The following will remove that filter and make sbt copy all files
// in "test/resources".
excludeFilter in (Test, unmanagedResources) := ""


lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
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

val akkaVersion = "2.5.17"
val akkaHttpVersion = "10.1.5"

lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.enablePlugins(IcosCpSbtDeployPlugin)
	.settings(
		name := "stiltcluster",
		version := "0.2.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-remote"         % akkaVersion,
			"com.typesafe.akka" %% "akka-slf4j"          % akkaVersion,
			"ch.qos.logback"     % "logback-classic"     % "1.1.3",
			"org.scalatest"     %% "scalatest"           % "3.0.1" % "test"
		),

		cpDeployTarget := "stiltcluster",
		cpDeployPlaybook := "stilt.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltcluster",

		fork in run := true,
		connectInput in run := true,

		baseDirectory in reStart := {
			baseDirectory.in(reStart).value.getParentFile
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
		version := "0.2.1",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"  %% "akka-http-spray-json"               % akkaHttpVersion,
			"com.typesafe.akka"  %% "akka-stream"                        % akkaVersion,
			"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.6.0-SNAPSHOT",
			"se.lu.nateko.cp"    %% "views-core"                         % "0.4.0-SNAPSHOT",
			"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.1.0-SNAPSHOT",
			"com.typesafe.akka"  %% "akka-testkit"                       % akkaVersion        % "test",
			"org.scalatest"      %% "scalatest"                          % "3.0.1"            % "test"
		),

		cpDeployTarget := "stiltweb",
		cpDeployPlaybook := "stilt.yml",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.stiltweb",

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
