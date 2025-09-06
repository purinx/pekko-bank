import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"

  lazy val pekkos = Seq(
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,
    "ch.qos.logback"    % "logback-classic"   % "1.5.6",
  )

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}
