package bank

import bank.actor.{BankActor, BankGuardian}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import scala.concurrent.duration._

class BankRoutes(supervisor: ActorRef[BankGuardian.Command])(using ActorSystem[?]) extends BankJsonSupport {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathEnd {
        post {
          entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>
            val result = supervisor.ask[BankActor.OperationResult] { ref =>
              BankGuardian.Deliver(BankActor.CreateAccount(ownerName, ref))
            }
            onSuccess(result) { case BankActor.CreateAccountSucceed(accountId) =>
              complete(StatusCodes.OK, accountId)
            }
          }
        }
      }
    }
}
