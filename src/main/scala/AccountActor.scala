import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object AccountActor {
  sealed trait Command
  final case class Apply(accountInfo: AccountInfo, replyTo: ActorRef[Event]) extends Command
  final case class Get(replyTo: ActorRef[AccountInfo])                       extends Command

  sealed trait Event
  final case class Applied(accountInfo: AccountInfo) extends Event
  final case class Rejected(reason: String)          extends Event

  def apply(
      accountId: AccountId,
      accountRepository: AccountRepository,
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      given ExecutionContext = context.executionContext
      context.log.info("Starting account actor for {}", accountId.value)
      var accountInfo: AccountInfo = AccountInfo(accountId, Nil)

      Behaviors.receiveMessage {
        case Get(replyTo) =>
          replyTo ! accountInfo
          Behaviors.same
        case Apply(info, replyTo) =>
          accountRepository.storeAccount(accountId, info).onComplete {
            case Success(_)  => replyTo ! Applied(info)
            case Failure(ex) => replyTo ! Rejected(ex.getMessage)
          }
          accountInfo = info
          Behaviors.same
      }
    }
  }
}
