import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"
  val pekkoHttpCirceVersion = "3.2.2" // For Scala 3
  val circeVersion = "0.14.9"
  val hikariCpVersion = "5.1.0"

  lazy val dbDependencies = Seq(
    "org.tpolecat" %% "doobie-core"     % "1.0.0-RC8",
    "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC8",
    "com.zaxxer"    % "HikariCP"        % hikariCpVersion
  )

  lazy val circeDependencies = Seq(
    "com.github.pjfanning" %% "pekko-http-circe" % pekkoHttpCirceVersion,
    "io.circe"             %% "circe-generic"    % circeVersion
  )

  lazy val commonDependencies = Seq(
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"      % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"        % pekkoHttpVersion,
    "org.typelevel"    %% "cats-core"         % "2.13.0",
    "dev.zio"          %% "zio"               % "2.1.21",
    "dev.zio"          %% "zio-interop-cats"  % "23.1.0.5",
    "ch.qos.logback"    % "logback-classic"   % "1.5.6",
    "com.typesafe"      % "config"            % "1.4.4"
  ) ++ circeDependencies ++ dbDependencies

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}