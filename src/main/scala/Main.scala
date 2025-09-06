import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.management.scaladsl.PekkoManagement

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main {
  private lazy val port = ConfigFactory.load().getInt("pekko.http.server.default-http-port")
  def main(args: Array[String]): Unit = {
    given ExecutionContext = scala.concurrent.ExecutionContext.global
    lazy val repository                    = new InMemoryAccountRepositoryImpl()
    lazy val routes = new AccountRoutes(system).routes

    given system: ActorSystem[AccountActorSupervisor.Command] =
      ActorSystem(
        AccountActorSupervisor(repository),
        "Account",
      )

    PekkoManagement(system).start().onComplete {
      case Success(uri) =>
        system.log.info(s"Pekko Management started at $uri")
      case Failure(exception) =>
        system.log.error("Failed to start Pekko Management", exception)
    }


    Http().newServerAt("localhost", port).bind(routes)
    system.log.info(s"Server online at http://localhost:${port}/")
  }
}
