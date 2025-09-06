import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"
  val pekkoManagementVersion = "1.1.1"

  lazy val pekkos = Seq(
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-cluster-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
    "org.apache.pekko" %% "pekko-management" % pekkoManagementVersion
  )

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}
