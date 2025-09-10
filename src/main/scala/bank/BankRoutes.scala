package bank

import bank.actor.{Bank, BankGuardian}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import scala.concurrent.duration._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

class BankRoutes(supervisor: ActorRef[BankGuardian.Command])(using ActorSystem[?]) extends BankJsonSupport {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathEnd {
        post {
          entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>
            val result = supervisor.ask[Bank.OperationResult] { ref =>
              BankGuardian.CreateAccount(ownerName)
            }
            onSuccess(result) { case Bank.CreateAccountSucceeded(accountId) =>
              complete(StatusCodes.OK, accountId)
            }
          }
        }
      }
    }
}
