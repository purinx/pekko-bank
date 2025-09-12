import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"
  val doobieVersion    = "1.0.0-RC8"

  lazy val commonDependencies = Seq(
    "org.apache.pekko" %% "pekko-actor-typed"     % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"          % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"            % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
    "org.typelevel"    %% "cats-core"             % "2.13.0",
    "dev.zio"          %% "zio"                   % "2.1.21",
    "dev.zio"          %% "zio-interop-cats"      % "23.1.0.5",
    "org.tpolecat"     %% "doobie-core"           % doobieVersion,
    "org.tpolecat"     %% "doobie-postgres"       % doobieVersion,
    "ch.qos.logback"    % "logback-classic"       % "1.5.6",
    "com.typesafe"      % "config"                % "1.4.4",
  )

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}
