import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Future
import scala.concurrent.duration._

class AccountRoutes(supervisor: ActorRef[AccountActorSupervisor.Command])(using ActorSystem[?]) {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathPrefix(Segment) { id =>
        val accountId = AccountId(id)
        path("credit" / IntNumber) { value =>
          post {
            val result: Future[AccountActor.Event] = supervisor.ask { ref =>
              AccountActorSupervisor.Deliver(
                AccountActor.Apply(AccountInfo(accountId, Seq(Credit(value))), ref),
                accountId,
              )
            }

            onSuccess(result) {
              case AccountActor.Applied(info) =>
                complete(StatusCodes.OK, s"Applied: $info")
              case AccountActor.Rejected(reason) =>
                complete(StatusCodes.BadRequest, s"Rejected: $reason")
            }
          }
        } ~
          path("debit" / IntNumber) { value =>
            post {
              val result: Future[AccountActor.Event] = supervisor.ask { ref =>
                AccountActorSupervisor.Deliver(
                  AccountActor.Apply(AccountInfo(accountId, Seq(Deposit(value))), ref),
                  accountId,
                )
              }

              onSuccess(result) {
                case AccountActor.Applied(info) =>
                  complete(StatusCodes.OK, s"Applied: $info")
                case AccountActor.Rejected(reason) =>
                  complete(StatusCodes.BadRequest, s"Rejected: $reason")
              }
            }
          } ~
          pathEnd {
            get {
              val result: Future[AccountInfo] = supervisor.ask { ref =>
                AccountActorSupervisor.Deliver(AccountActor.Get(ref), accountId)
              }
              onSuccess(result) { info =>
                complete(StatusCodes.OK, info.toString)
              }
            }
          }
      }
    }
}
