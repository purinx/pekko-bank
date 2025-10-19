package bank.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import bank.domain.account.AccountId

object BankGuardian {
  sealed trait Command
  final case class Deliver(command: AccountBehavior.Command, to: String) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Deliver(command, to) => {
          val target: ActorRef[AccountBehavior.Command] =
            context.child(to) match {
              case Some(ref) => ref.unsafeUpcast[AccountBehavior.Command]
              case None      => context.spawn(AccountBehavior(to), to)
            }
          target ! command
          Behaviors.same
        }
      }
    }
  }
}
