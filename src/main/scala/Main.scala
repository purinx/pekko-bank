import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.management.scaladsl.PekkoManagement

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main {
  private lazy val port               = ConfigFactory.load().getInt("pekko.http.server.default-http-port")
  def main(args: Array[String]): Unit = {
    given ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    val repository             = new InMemoryAccountRepositoryImpl()

    given system: ActorSystem[AccountActorSupervisor.Command] =
      ActorSystem(
        AccountActorSupervisor(repository),
        "Account",
      )

    val routes = new AccountRoutes(system).routes

    PekkoManagement(system).start().onComplete {
      case Success(uri) =>
        system.log.info(s"Pekko Management started at $uri")
      case Failure(exception) =>
        system.log.error("Failed to start Pekko Management", exception)
    }

    Http().newServerAt("localhost", port).bind(routes)
    system.log.info(s"Server online at http://localhost:${port}/")
    println("Press RETURN to stop...")
    val _ = StdIn.readLine()
    system.terminate()
  }
}
