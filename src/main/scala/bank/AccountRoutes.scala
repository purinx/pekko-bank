package bank

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._

import scala.concurrent.duration._

class AccountRoutes(supervisor: ActorRef[BankGuardian.Command])(using ActorSystem[?]) {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathPrefix(Segment) { id =>
        val accountId = id
        path("withdraw" / IntNumber) { value =>
          post {
            val result = supervisor.ask[BankAccount.OperationResult] { ref =>
              BankGuardian.Deliver(BankAccount.Withdraw(value, ref), accountId)
            }
            onSuccess(result) {
              case BankAccount.OperationSucceeded(balance) =>
                complete(StatusCodes.OK, s"withdraw operation succeeded: newbalance: ${balance}")
              case BankAccount.OperationFailed(reason) =>
                complete(StatusCodes.BadRequest, s"reason: ${reason}")
            }
          }
        } ~ path("deposit" / IntNumber) { value =>
          val result = supervisor.ask[BankAccount.OperationResult] { ref =>
            BankGuardian.Deliver(BankAccount.Deposit(value, ref), accountId)
          }
          onSuccess(result) {
            case BankAccount.OperationSucceeded(balance) =>
              complete(StatusCodes.OK, s"deposit operation succeeded: newbalance: ${balance}")
            case BankAccount.OperationFailed(reason) =>
              complete(StatusCodes.BadRequest, s"reason: ${reason}")
          }
        } ~ path("balance") {
          get {
            val result = supervisor.ask[BankAccount.CurrentBalance] { ref =>
              BankGuardian.Deliver(BankAccount.GetBalance(ref), accountId)
            }
            onSuccess(result) { bal =>
              complete(StatusCodes.OK, s"balance: ${bal.balance}")
            }
          }
        }
      }
    }
}
