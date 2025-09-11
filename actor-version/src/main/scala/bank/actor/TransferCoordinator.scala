package bank.actor

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import java.util.UUID

object TransferCoordinator {

  sealed trait Command
  final case class Transfer(
      sourceAccountNumber: String,
      destinationAccountNumber: String,
      amount: BigDecimal,
      replyTo: ActorRef[TransferResult],
  ) extends Command

  sealed trait TransferResult
  case object TransferSucceeded                   extends TransferResult
  final case class TransferFailed(reason: String) extends TransferResult

  // private messages for Saga steps
  private sealed trait InternalCommand extends Command
  private case class WithdrawalSucceeded(
      source: String,
      dest: String,
      amount: BigDecimal,
      replyTo: ActorRef[TransferResult],
  ) extends InternalCommand
  private case class WithdrawalFailed(reason: String, replyTo: ActorRef[TransferResult]) extends InternalCommand
  private case class DepositSucceeded(replyTo: ActorRef[TransferResult])                 extends InternalCommand
  private case class DepositFailed(
      reason: String,
      source: String,
      amount: BigDecimal,
      replyTo: ActorRef[TransferResult],
  ) extends InternalCommand
  private case class CompensationSucceeded(replyTo: ActorRef[TransferResult])              extends InternalCommand
  private case class CompensationFailed(reason: String, replyTo: ActorRef[TransferResult]) extends InternalCommand

  def apply(bankGuardian: ActorRef[Guardian.Command]): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Transfer(source, dest, amount, replyTo) =>
          context.log.info(s"Transfer request received: $source -> $dest, amount: $amount")
          val withdrawAdapter = context.messageAdapter[AccountActor.OperationResult] {
            case AccountActor.OperationSucceeded(_) => WithdrawalSucceeded(source, dest, amount, replyTo)
            case AccountActor.OperationFailed(r)    => WithdrawalFailed(r, replyTo)
          }
          bankGuardian ! Guardian.Deliver(AccountActor.Withdraw(amount, withdrawAdapter), source)
          Behaviors.same
        case WithdrawalSucceeded(source, dest, amount, replyTo) =>
          context.log.info(s"Withdrawal from $source succeeded.")
          val depositAdapter = context.messageAdapter[AccountActor.OperationResult] {
            case AccountActor.OperationSucceeded(_) => DepositSucceeded(replyTo)
            case AccountActor.OperationFailed(r)    => DepositFailed(r, source, amount, replyTo)
          }
          bankGuardian ! Guardian.Deliver(AccountActor.Deposit(amount, depositAdapter), dest)
          Behaviors.same
        case WithdrawalFailed(reason, replyTo) =>
          context.log.error(s"Withdrawal failed: $reason")
          replyTo ! TransferFailed(s"Withdrawal failed: $reason")
          Behaviors.stopped
        case DepositSucceeded(replyTo) =>
          context.log.info(s"Deposit to destination succeeded.")
          replyTo ! TransferSucceeded
          Behaviors.stopped
        case DepositFailed(reason, source, amount, replyTo) =>
          context.log.error(s"Deposit failed: $reason. Starting compensation.")
          val compensationAdapter = context.messageAdapter[AccountActor.OperationResult] {
            case AccountActor.OperationSucceeded(_) => CompensationSucceeded(replyTo)
            case AccountActor.OperationFailed(r)    => CompensationFailed(r, replyTo)
          }
          bankGuardian ! Guardian.Deliver(AccountActor.Deposit(amount, compensationAdapter), source)
          Behaviors.same
        case CompensationSucceeded(replyTo) =>
          context.log.info("Compensation deposit succeeded.")
          replyTo ! TransferFailed("Transfer failed, but funds returned to source account.")
          Behaviors.stopped
        case CompensationFailed(reason, replyTo) =>
          context.log.error(s"FATAL: Compensation deposit failed: $reason. Manual intervention required.")
          replyTo ! TransferFailed(s"CRITICAL ERROR: Transfer failed and compensation failed: $reason")
          Behaviors.stopped
      }
    }
  }
}
