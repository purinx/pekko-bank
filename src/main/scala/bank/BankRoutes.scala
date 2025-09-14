package bank

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import bank.actor.{AccountBehavior, BankGuardian}

import scala.concurrent.duration._
import bank.domain.account.Account
import bank.repository.AccountRepository
import bank.util.db.DBIORunner
import zio.{Unsafe, Runtime}

class BankRoutes(
                  supervisor: ActorRef[BankGuardian.Command],
                  accountRepository: AccountRepository,
                  dbioRunner: DBIORunner,
)(using ActorSystem[?])
    extends BankJsonSupport {
  private given Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("account") {
      pathEnd {
        extractExecutionContext { implicit ec =>
          post {
            entity(as[CreateAccountRequest]) { case CreateAccountRequest(ownerName) =>
              val account = Account.create(ownerName)
              val io      = dbioRunner.runTx {
                accountRepository.create(account)
              }

              val future = Unsafe.unsafe { implicit unsafe =>
                Runtime.default.unsafe.runToFuture(io)
              }

              onSuccess(future) { _ =>
                complete(StatusCodes.OK, account.id.value.toString)
              }
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
                complete(StatusCodes.OK, s"withdraw operation succeeded: newbalance: ${balance}")
              case AccountBehavior.OperationFailed(reason) =>
                complete(StatusCodes.BadRequest, s"reason: ${reason}")
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
          }
        } ~ path("balance") {
          get {
            val result = supervisor.ask[AccountBehavior.CurrentBalance] { ref =>
              BankGuardian.Deliver(AccountBehavior.GetBalance(ref), accountId)
            }
            onSuccess(result) { bal =>
              complete(StatusCodes.OK, s"balance: ${bal.balance}")
            }
          }
        }
      }
    }
}
