val scala3Version = "3.7.2"
val pekkoVersion = "1.1.5"
val pekkoHttpVersion = "1.2.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "pekko-bank",
    version := "0.1.0",
    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion
    )
  )
