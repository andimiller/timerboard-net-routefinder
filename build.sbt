ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .enablePlugins(UniversalPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "timerboard-net-routefinder",
    version := "0.1",
    dockerExposedPorts := Seq(8080),
    dockerRepository := Some("andimiller"),
    dockerExecCommand := Seq("podman"),
    Universal / mappings += file("config.json") -> "config.json",
    dockerBaseImage := "openjdk:17",
    libraryDependencies ++= List(
      "net.andimiller"                %% "hedgehogs-core"      % "0.2.0",
      "net.andimiller"                %% "hedgehogs-circe"     % "0.2.0",
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"  % "0.2.1",
      "com.monovore"                  %% "decline"             % "2.2.0",
      "org.http4s"                    %% "http4s-ember-server" % "0.23.12",
      "ch.qos.logback"                 % "logback-classic"     % "1.2.3",
      "com.github.cb372"              %% "scalacache-caffeine" % "1.0.0-M6"
    ),
    libraryDependencies ++= List(
      "http4s-server",
      "json-circe",
      "openapi-docs",
      "redoc",
      "prometheus-metrics",
      "opentelemetry-metrics"
    ).map { module =>
      "com.softwaremill.sttp.tapir" %% s"tapir-$module" % "1.0.0"
    }
  )
