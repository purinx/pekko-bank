import Dependencies.*

ThisBuild / scalaVersion := "3.7.2"

lazy val options = Seq(
  "-deprecation",
  "-feature",
  "-Wvalue-discard",
  "-Wunused:all",
  "-language:experimental.macros",
)

lazy val commonSettings = Seq(
  scalacOptions ++= options,
  Compile / console / scalacOptions ~= { _.filterNot(_ == "-Wunused:all") },
  scalafmtOnCompile := true,
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "pekko-bank",
    publish / skip := true
  )
  .aggregate(
    rdbVersion,
    actorVersion
  )

lazy val rdbVersion = project
  .in(file("rdb-version"))
  .settings(
    name    := "rdb-version",
    version := "0.1.0",
    commonSettings,
    libraryDependencies ++= commonDependencies,
    libraryDependencies += munit % Test,
  )

lazy val actorVersion = project
  .in(file("actor-version"))
  .settings(
    name    := "actor-version",
    version := "0.1.0",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
      "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,
      "ch.qos.logback"    % "logback-classic"   % "1.5.6",
      "com.typesafe"      % "config"            % "1.4.4",
      munit % Test
    ) ++ circeDependencies
  )
