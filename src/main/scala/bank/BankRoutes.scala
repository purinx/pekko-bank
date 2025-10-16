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
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route = extractExecutionContext { implicit ec =>
    pathPrefix("account") {
      pathEnd {
        post {
          entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>
            val result = supervisor.ask[AccountBehavior.OperationResult] { ref =>
              val accountId = AccountId.newId()

              BankGuardian.Deliver(AccountBehavior.Create(ownerName, ref), accountId.asString)
            }

            onSuccess(result) {
              case AccountBehavior.AccountCreated(account) =>
                complete(StatusCodes.OK, account.id.value.toString)
              case _ =>
                complete(StatusCodes.BadRequest)
            }
          }
        }
        // TODO: クエリ実装する
        // ~ get {
        //   val io = dbioRunner.runTx {
        //     accountRepository.all()
        //   }

        //   val future = Unsafe.unsafe { implicit unsafe =>
        //     Runtime.default.unsafe.runToFuture(io)
        //   }

        //   onSuccess(future) { accounts =>
        //     complete(
        //       StatusCodes.OK,
        //       ListAccountResponse(accounts.map(ListAccountResponseItem.fromAccount(_))),
        //     )
        //   }
        // }
      } ~ pathPrefix(Segment) { id =>
        val accountId = id
        path("withdraw" / IntNumber) { value =>
          post {
            val result = supervisor.ask[AccountBehavior.OperationResult] { ref =>
              BankGuardian.Deliver(AccountBehavior.Withdraw(value, ref), accountId)
            }
            onSuccess(result) {
              case AccountBehavior.OperationSucceeded(balance) =>
                complete(StatusCodes.OK, s"withdraw operation succeeded: newbalance: ${balance}")
              case AccountBehavior.OperationFailed(reason) =>
                complete(StatusCodes.BadRequest, s"reason: ${reason}")
              case _ =>
                complete(StatusCodes.BadRequest)
            }
          }
        } ~ path("deposit" / IntNumber) { value =>
          val result = supervisor.ask[AccountBehavior.OperationResult] { ref =>
            BankGuardian.Deliver(AccountBehavior.Deposit(value, ref), accountId)
          }
          onSuccess(result) {
            case AccountBehavior.OperationSucceeded(balance) =>
              complete(StatusCodes.OK, s"deposit operation succeeded: newbalance: ${balance}")
            case AccountBehavior.OperationFailed(reason) =>
              complete(StatusCodes.BadRequest, s"reason: ${reason}")
            case _ =>
              complete(StatusCodes.BadRequest)
          }
        }
        // TODO: クエリ実装
        // ~ path("balance") {
        //   get {
        //   }
        // }
      }
    }
  }
}
