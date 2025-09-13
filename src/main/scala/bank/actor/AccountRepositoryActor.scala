package bank.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import bank.domain.account.Account
import bank.repository.AccountRepository
import bank.util.db.DBIORunner
import scala.util.{Success, Failure}
import zio.{Unsafe, Runtime}
import java.util.UUID

object AccountRepositoryActor {

  sealed trait Command
  final case class CreateAccount(messageId: UUID, ownerName: String, replyTo: ActorRef[CreateAccountResult])
      extends Command

  sealed trait CreateAccountResult {
    def messageId: UUID
  }
  final case class CreateAccountSuccess(account: Account, messageId: UUID) extends CreateAccountResult
  final case class CreateAccountFailure(ownerName: String, errorMessage: String, messageId: UUID)
      extends CreateAccountResult
  private final case class WrappedCreateAccountResult(
      result: CreateAccountResult,
      replyTo: ActorRef[CreateAccountResult],
  ) extends Command

  private val MaxOperationsInProgress = 10

  def apply(
      accountRepository: AccountRepository,
      dbioRunner: DBIORunner,
  ): Behavior[Command] = next(accountRepository, dbioRunner, operationsInProgress = 0)

  private def next(
      accountRepository: AccountRepository,
      dbioRunner: DBIORunner,
      operationsInProgress: Int,
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case CreateAccount(messageId, ownerName, replyTo) =>
          if (operationsInProgress == MaxOperationsInProgress) {
            replyTo ! CreateAccountFailure(
              ownerName,
              s"Max $MaxOperationsInProgress concurrent operations supported",
              messageId,
            )
            Behaviors.same
          } else {

            val account = Account.create(ownerName)
            val io      = dbioRunner.runTx {
              accountRepository.create(account)
            }
            val futureResult = Unsafe.unsafe { implicit unsafe =>
              Runtime.default.unsafe.runToFuture(io)
            }

            context.pipeToSelf(futureResult) {
              // map the Future value to a message, handled by this actor
              case Success(_) => WrappedCreateAccountResult(CreateAccountSuccess(account, messageId), replyTo)
              case Failure(e) =>
                WrappedCreateAccountResult(CreateAccountFailure(ownerName, e.getMessage, messageId), replyTo)
            }
            // increase operationsInProgress counter
            next(accountRepository, dbioRunner, operationsInProgress + 1)
          }

        case WrappedCreateAccountResult(result, replyTo) =>
          // send result to original requestor
          replyTo ! result
          // decrease operationsInProgress counter
          next(accountRepository, dbioRunner, operationsInProgress - 1)
      }
    }
  }

}
