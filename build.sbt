import Dependencies.*

ThisBuild / scalaVersion := "3.7.2"

lazy val commonSettings = Seq(
  scalacOptions ++= "-deprecation" :: "-feature" :: "-Wvalue-discard" :: "-Wunused:all" :: Nil,
  Compile / console / scalacOptions ~= { _.filterNot(_ == "-Wunused:all") },
  scalafmtOnCompile := true,
)

lazy val root = project
  .in(file("."))
  .settings(
    name    := "pekko-bank",
    version := "0.1.0",
    mainClass := Some("Main"),
    commonSettings,
    libraryDependencies ++= pekkos,
    libraryDependencies += munit % Test,
  )