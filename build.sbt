name := "stiltweb"

version := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
	"com.typesafe.akka"  %% "akka-slf4j"                         % "2.4.16",
	"ch.qos.logback"      % "logback-classic"                    % "1.1.2",
	"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.5-SNAPSHOT",
	"se.lu.nateko.cp"    %% "views-core"                         % "0.2-SNAPSHOT",
	"se.lu.nateko.cp"    %% "data-netcdf"                        % "0.1.0-SNAPSHOT",
	"org.scalatest"      %% "scalatest"                          % "3.0.0"            % "test"
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

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

