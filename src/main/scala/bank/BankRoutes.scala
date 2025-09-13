package bank

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import bank.util.actor.FilterableAskPattern.*
import bank.util.actor.FilterableAskPattern
import bank.actor.{AccountActor, BankGuardian}
import java.util.UUID

import scala.concurrent.duration._
import bank.actor.AccountRepositoryActor

class BankRoutes(
    supervisor: ActorRef[BankGuardian.Command],
)(using ActorSystem[?])
    extends BankJsonSupport {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathEnd {
        extractExecutionContext { implicit ec =>
          post {
            entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>

              val messageId = UUID.randomUUID
              val result    = supervisor.askExpecting[AccountRepositoryActor.CreateAccountResult] { ref =>
                BankGuardian.CreateAccount(messageId, ownerName, ref)
              }(predicate = _.messageId == messageId)

              onSuccess(result) {
                case AccountRepositoryActor.CreateAccountSuccess(account, _) =>
                  complete(StatusCodes.OK, account.id.value.toString)
                case AccountRepositoryActor.CreateAccountFailure(ownerName, errorMessage, _) =>
                  complete(StatusCodes.BadRequest, s"create account of $ownerName failure. reason: $errorMessage")
              }
            }
          }
        }
      } ~ pathPrefix(Segment) { id =>
        val accountId = id
        path("withdraw" / IntNumber) { value =>
          post {
            val result = supervisor.ask[AccountActor.OperationResult] { ref =>
              BankGuardian.Deliver(AccountActor.Withdraw(value, ref), accountId)
            }
            onSuccess(result) {
              case AccountActor.OperationSucceeded(balance) =>
                complete(StatusCodes.OK, s"withdraw operation succeeded: newbalance: ${balance}")
              case AccountActor.OperationFailed(reason) =>
                complete(StatusCodes.BadRequest, s"reason: ${reason}")
            }
          }
        } ~ path("deposit" / IntNumber) { value =>
          val result = supervisor.ask[AccountActor.OperationResult] { ref =>
            BankGuardian.Deliver(AccountActor.Deposit(value, ref), accountId)
          }
          onSuccess(result) {
            case AccountActor.OperationSucceeded(balance) =>
              complete(StatusCodes.OK, s"deposit operation succeeded: newbalance: ${balance}")
            case AccountActor.OperationFailed(reason) =>
              complete(StatusCodes.BadRequest, s"reason: ${reason}")
          }
        } ~ path("balance") {
          get {
            val result = supervisor.ask[AccountActor.CurrentBalance] { ref =>
              BankGuardian.Deliver(AccountActor.GetBalance(ref), accountId)
            }
            onSuccess(result) { bal =>
              complete(StatusCodes.OK, s"balance: ${bal.balance}")
            }
          }
        }
      }
    }
}
