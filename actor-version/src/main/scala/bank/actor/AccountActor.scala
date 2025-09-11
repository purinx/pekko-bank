package bank.actor

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object AccountActor {

  // --- Commands ---
  sealed trait Command
  final case class Deposit(amount: BigDecimal, replyTo: ActorRef[OperationResult])  extends Command
  final case class Withdraw(amount: BigDecimal, replyTo: ActorRef[OperationResult]) extends Command
  final case class GetBalance(replyTo: ActorRef[CurrentBalance])                    extends Command

  // --- Replies ---
  sealed trait OperationResult
  final case class OperationSucceeded(newBalance: BigDecimal) extends OperationResult
  final case class OperationFailed(reason: String)            extends OperationResult

  final case class CurrentBalance(balance: BigDecimal)

  def apply(accountId: String, balance: BigDecimal = 0): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Deposit(amount, replyTo) =>
          if (amount <= 0) {
            val reason = "Amount must be positive."
            context.log.warn(s"[$accountId] Deposit failed: $amount. Reason: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else {
            val newBalance = balance + amount
            context.log.info(s"[$accountId] Deposited $amount. New balance: $newBalance")
            replyTo ! OperationSucceeded(newBalance)
            apply(accountId, newBalance)
          }
        case Withdraw(amount, replyTo) =>
          if (amount <= 0) {
            val reason = "Amount must be positive."
            context.log.warn(s"[$accountId] Withdraw failed: $amount. Reason: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else if (balance < amount) {
            val reason = s"Insufficient funds. Balance: $balance, Amount: $amount"
            context.log.warn(s"[$accountId] Withdraw failed: $amount. Reason: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else {
            val newBalance = balance - amount
            context.log.info(s"[$accountId] Withdrew $amount. New balance: $newBalance")
            replyTo ! OperationSucceeded(newBalance)
            apply(accountId, newBalance)
          }
        case GetBalance(replyTo) =>
          context.log.info(s"[$accountId] Balance check. Current balance: $balance")
          replyTo ! CurrentBalance(balance)
          Behaviors.same
      }
    }
}
