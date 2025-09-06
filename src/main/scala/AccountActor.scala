import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object AccountActor {
  sealed trait Command
  final case class Apply(ledger: AccountLedger, replyTo: ActorRef[Event]) extends Command
  final case class Get(replyTo: ActorRef[AccountInfo])                    extends Command
  private final case class Loaded(accountInfo: AccountInfo)               extends Command
  private final case class PersistFailed(ex: Throwable)                   extends Command

  sealed trait Event
  final case class Applied(accountInfo: AccountInfo) extends Event
  final case class Rejected(reason: String)          extends Event

  def apply(
      accountId: AccountId,
      accountRepository: AccountRepository,
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info("Starting account actor for {}", accountId.value)

      context.pipeToSelf(accountRepository.findBy(accountId)) {
        case Success(Some(info)) => Loaded(info)
        case Success(None)       => Loaded(AccountInfo(accountId, Nil))
        case Failure(ex)         => PersistFailed(ex)
      }

      loading(accountRepository)
    }
  }

  private def loading(accountRepository: AccountRepository): Behavior[Command] = {
    Behaviors.receiveMessage {
      case Loaded(info)      => running(accountRepository, info)
      case PersistFailed(ex) => throw ex
      case _                 => Behaviors.unhandled
    }
  }

  private def running(accountRepository: AccountRepository, accountInfo: AccountInfo): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      given ExecutionContext = context.executionContext
      message match {
        case Get(replyTo) =>
          replyTo ! accountInfo
          Behaviors.same
        case Apply(ledger, replyTo) =>
          val updatedInfo = accountInfo.applyLedger(ledger)
          accountRepository
            .storeAccount(accountInfo.accountId, updatedInfo)
            .onComplete {
              case Success(_) => replyTo ! Applied(updatedInfo)
              case Failure(ex) => replyTo ! Rejected(ex.getMessage)
            }
          running(accountRepository, updatedInfo)
        case _: Loaded =>
          Behaviors.unhandled
        case _: PersistFailed =>
          Behaviors.unhandled
      }
    }
  }
}
