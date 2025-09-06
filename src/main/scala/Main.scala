import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.management.scaladsl.PekkoManagement

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    given ExecutionContext = scala.concurrent.ExecutionContext.global
    lazy val repository                    = new InMemoryAccountRepositoryImpl()

    given ActorSystem[AccountActorSupervisor.Command] =
      ActorSystem(
        AccountActorSupervisor(repository),
        "Account",
      )

    PekkoManagement(summon[ActorSystem[AccountActorSupervisor.Command]]).start().onComplete {
      case Success(uri) =>
        summon[ActorSystem[AccountActorSupervisor.Command]].log.info(s"Pekko Management started at $uri")
      case Failure(exception) =>
        summon[ActorSystem[AccountActorSupervisor.Command]].log.error("Failed to start Pekko Management", exception)
    }
  }
}
