package bank.actor

import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object Guardian {

  sealed trait Command
  // For AccountActor
  final case class Deliver(command: AccountActor.Command, to: String) extends Command
  // For Transfer
  final case class ProcessTransfer(
      source: String,
      dest: String,
      amount: BigDecimal,
      replyTo: ActorRef[TransferCoordinator.TransferResult],
  ) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Deliver(command, to) =>
          val target: ActorRef[AccountActor.Command] =
            context.child(to) match {
              case Some(ref) => ref.unsafeUpcast[AccountActor.Command]
              case None      => context.spawn(AccountActor(to), to)
            }
          target ! command
          Behaviors.same

        case ProcessTransfer(source, dest, amount, replyTo) =>
          val coordinator = context.spawn(
            Behaviors.supervise(TransferCoordinator(context.self)).onFailure(SupervisorStrategy.stop),
            s"transfer-coordinator-${java.util.UUID.randomUUID()}",
          )
          coordinator ! TransferCoordinator.Transfer(source, dest, amount, replyTo)
          Behaviors.same
      }
    }
  }
}
