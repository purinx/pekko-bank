import sbt.*

object Dependencies {

  val pekkoVersion     = "1.1.5"
  val pekkoHttpVersion = "1.2.0"
  val hikariCpVersion  = "5.1.0"
  val postgresVersion  = "42.7.7"

  lazy val dbDependencies = Seq(
    "org.tpolecat"  %% "doobie-core"     % "1.0.0-RC8",
    "org.tpolecat"  %% "doobie-postgres" % "1.0.0-RC8",
    "com.zaxxer"     % "HikariCP"        % hikariCpVersion,
    "org.postgresql" % "postgresql"      % postgresVersion % Runtime,
  )

  lazy val commonDependencies = Seq(
    "org.apache.pekko" %% "pekko-actor-typed"         % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"              % pekkoVersion,
    "org.apache.pekko" %% "pekko-http"                % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json"     % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-persistence-typed"   % pekkoVersion,
    "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
    "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % Test,
    "org.typelevel"    %% "cats-core"                 % "2.13.0",
    "dev.zio"          %% "zio"                       % "2.1.21",
    "dev.zio"          %% "zio-interop-cats"          % "23.1.0.5",
    "org.tpolecat"     %% "doobie-core"               % "1.0.0-RC8",
    "org.tpolecat"     %% "doobie-postgres"           % "1.0.0-RC8",
    "ch.qos.logback"    % "logback-classic"           % "1.5.6",
    "com.typesafe"      % "config"                    % "1.4.4",
  ) ++ dbDependencies

  lazy val munit = "org.scalameta" %% "munit" % "1.0.0"
}
