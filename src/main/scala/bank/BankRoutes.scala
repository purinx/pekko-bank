package bank

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import bank.actor.{AccountBehavior, BankGuardian}

import scala.concurrent.duration._
import bank.domain.account.AccountId

class BankRoutes(supervisor: ActorRef[BankGuardian.Command])(using ActorSystem[?]) extends BankJsonSupport {
  private given Timeout = Timeout(100.seconds)

  lazy val routes: Route = extractExecutionContext { implicit ec =>
    pathPrefix("account") {
      pathEnd {
        post {
          entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>
            val accountId = AccountId.newId()
            val result    = supervisor.ask[AccountBehavior.OperationResult] { ref =>
              BankGuardian.Deliver(AccountBehavior.Create(ownerName, ref), accountId.asString)
            }

            onSuccess(result) {
              case AccountBehavior.AccountCreated(account) =>
                complete(StatusCodes.OK, account.id.value.toString)
              case _ =>
                complete(StatusCodes.InternalServerError, AccountOperationFailureResponse("Unknown message from Actor"))
            }
          }
        }
      } ~ pathPrefix(Segment) { id =>
        val accountId = id
        path("withdraw" / IntNumber) { value =>
          post {
            val result = supervisor.ask[AccountBehavior.OperationResult] { ref =>
              BankGuardian.Deliver(AccountBehavior.Withdraw(value, ref), accountId)
            }
            onSuccess(result) {
              case AccountBehavior.OperationSucceeded(balance) =>
                complete(StatusCodes.OK, AccountOperationSuccessResponse(balance))
              case AccountBehavior.OperationFailed(reason) =>
                complete(StatusCodes.BadRequest, AccountOperationFailureResponse(reason))
              case _ =>
                complete(StatusCodes.InternalServerError, AccountOperationFailureResponse("Unknown message from Actor"))
            }
          }
        } ~ path("deposit" / IntNumber) { value =>
          val result = supervisor.ask[AccountBehavior.OperationResult] { ref =>
            BankGuardian.Deliver(AccountBehavior.Deposit(value, ref), accountId)
          }
          onSuccess(result) {
            case AccountBehavior.OperationSucceeded(balance) =>
              complete(StatusCodes.OK, AccountOperationSuccessResponse(balance))
            case AccountBehavior.OperationFailed(reason) =>
              complete(StatusCodes.BadRequest, AccountOperationFailureResponse(reason))
            case _ =>
              complete(StatusCodes.BadRequest)
          }
        }
        // TODO: クエリ実装
        // ~ path("balance") {
        //   get { }
        // }
        // ~ path("ledger") {
        //   get { }
        // }
      }
    }
  }
}
