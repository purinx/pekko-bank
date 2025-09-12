package bank.actor

import bank.domain.account.AccountId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.PersistenceId

object BankGuardian {
  sealed trait Command
  final case class Deliver(command: AccountActor.Command, to: String) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Deliver(command, to) => {
          val target: ActorRef[AccountActor.Command] =
            context.child(to) match {
              case Some(ref) => ref.unsafeUpcast[AccountActor.Command]
              case None      => context.spawn(AccountActor(to), to)
            }
          target ! command
          Behaviors.same
        }
      }
    }
  }
}
