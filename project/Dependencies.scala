import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"
  val pekkoManagementVersion = "1.1.1"
  val slf4jVersion = "2.0.13"

  lazy val pekkos = Seq(
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-cluster-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
    "org.apache.pekko" %% "pekko-management" % pekkoManagementVersion,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
    "org.slf4j" % "slf4j-simple" % slf4jVersion
  )

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}
