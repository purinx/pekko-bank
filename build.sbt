import Dependencies.*

ThisBuild / scalaVersion := "3.7.2"

lazy val options = Seq(
  "-deprecation",
  "-feature",
  "-Wvalue-discard",
  "-Wunused:all",
  "-language:experimental.macros",
  "-Vimplicits",
)

lazy val commonSettings = Seq(
  scalacOptions ++= options,
  Compile / console / scalacOptions ~= { _.filterNot(_ == "-Wunused:all") },
  scalafmtOnCompile := true,
)

lazy val root = project
  .in(file("."))
  .settings(
    name    := "pekko-bank",
    version := "0.1.0",
    commonSettings,
    libraryDependencies ++= commonDependencies,
    libraryDependencies += munit % Test,
  )
