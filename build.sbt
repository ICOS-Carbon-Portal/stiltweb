name := "stiltweb"

version := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
	"com.typesafe.akka"  %% "akka-http-spray-json-experimental"  % "2.4.7",
	"com.typesafe.akka"  %% "akka-slf4j"                         % "2.4.7",
	"ch.qos.logback"      % "logback-classic"                    % "1.1.2",
	"se.lu.nateko.cp"    %% "cpauth-core"                        % "0.2",
	"org.scalatest"      %% "scalatest"        % "2.2.1"         % "test"
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

assemblyMergeStrategy in assembly := {
  case "application.conf"                            => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

Revolver.settings

