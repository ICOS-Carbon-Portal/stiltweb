name := "stiltweb"

version := "0.1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	"com.typesafe.akka"  %% "akka-http-core-experimental"        % "2.0.2",
	"com.typesafe.akka"  %% "akka-http-spray-json-experimental"  % "2.0.2",
	"com.typesafe.akka"  %% "akka-slf4j"                         % "2.3.14",
	"ch.qos.logback"      % "logback-classic"                    % "1.1.2",
	"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.2",

	"org.scalatest"      %% "scalatest"        % "2.2.1" % "test"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

Revolver.settings

