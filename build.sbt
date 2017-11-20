cancelable in Global := true

// By default sbt will exclude files starting with a dot when copying
// resources. The following will remove that filter and make sbt copy all files
// in "test/resources".
excludeFilter in (Test, unmanagedResources) := ""


lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := "2.12.4",
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

val akkaVersion = "2.4.20"
val akkaHttpVersion = "10.0.10"


lazy val buildFrontend = taskKey[Unit]("Builds the front end projects")
lazy val frontendThenAssembly = taskKey[File]("Builds the front end projects and then runs assembly")
lazy val deploy = inputKey[Unit]("Deploys to production using Ansible (depends on 'infrastructure' project)")


lazy val stiltcluster = (project in file("stiltcluster"))
	.settings(commonSettings: _*)
	.settings(
		name := "stiltcluster",
		version := "0.1.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-cluster"     % akkaVersion,
			"com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
			"com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
			"org.scalatest"     %% "scalatest"                          % "3.0.1"            % "test"
		),
		fork in run := true,
		connectInput in run := true
	)

val npmPublish = taskKey[Unit]("runs 'npm publish'")
npmPublish := Process("npm run publish").!

lazy val stiltweb = (project in file("."))
	.dependsOn(stiltcluster)
	.enablePlugins(SbtTwirl,IcosCpSbtDeployPlugin)
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
