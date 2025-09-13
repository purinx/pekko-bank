package bank.actor

import bank.domain.account.AccountId
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object AccountActor {

  // --- AccountActor が受け取るメッセージ ---
  sealed trait Command
  final case class Deposit(amount: Long, replyTo: ActorRef[OperationResult])  extends Command
  final case class Withdraw(amount: Long, replyTo: ActorRef[OperationResult]) extends Command
  final case class GetBalance(replyTo: ActorRef[CurrentBalance])              extends Command

  // --- AccountActor が発行するメッセージ ---
  sealed trait Event
  final case class Deposited(amount: Long) extends Event
  final case class Withdrew(amount: Long)  extends Event
  final case class GotBalance()            extends Event

  // --- AccountActor が保持する状態 ---
  final case class BalanceState(history: List[Balance]) {
    def currentBalance: CurrentBalance = CurrentBalance(history.map(_.value).sum)
  }
  final case class Balance(value: Long)

  // --- AccountActor が返信するメッセージ ---
  sealed trait OperationResult
  final case class OperationSucceeded(newBalance: Long) extends OperationResult
  final case class OperationFailed(reason: String)      extends OperationResult
  final case class CurrentBalance(balance: Long)

  def apply(accountId: String, balance: Long = 0L): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Deposit(amount, replyTo) =>
          if (amount <= 0) {
            val reason = "入金額は正の数でなければなりません。"
            context.log.warn(s"[$accountId] 入金失敗: $amount. 理由: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else {
            val newBalance = balance + amount
            context.log.info(s"[$accountId] $amount 円入金しました。新残高: $newBalance 円")
            replyTo ! OperationSucceeded(newBalance)
            apply(accountId, newBalance)
          }
        case Withdraw(amount, replyTo) =>
          if (amount <= 0) {
            val reason = "出金額は正の数でなければなりません。"
            context.log.warn(s"[$accountId] 出金失敗: $amount. 理由: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else if (balance < amount) {
            val reason = s"残高が不足しています。残高: $balance 円, 出金額: $amount 円"
            context.log.warn(s"[$accountId] 出金失敗: $amount. 理由: $reason")
            replyTo ! OperationFailed(reason)
            Behaviors.same
          } else {
            val newBalance = balance - amount
            context.log.info(s"[$accountId] $amount 円出金しました。新残高: $newBalance 円")
            replyTo ! OperationSucceeded(newBalance)
            apply(accountId, newBalance)
          }
        case GetBalance(replyTo) =>
          context.log.info(s"[$accountId] 残高照会。現在の残高: $balance 円")
          replyTo ! CurrentBalance(balance)
          Behaviors.same
      }
    }

  def applyEventSourcedBehavior(accountId: AccountId): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      EventSourcedBehavior[Command, Event, BalanceState](
        persistenceId = PersistenceId.ofUniqueId(accountId.asString),
        emptyState = AccountActor.BalanceState.empty,
        commandHandler = commandHandlerWithContext(accountId, context),
        eventHandler = eventHandler,
      )
    }

  private def commandHandlerWithContext(
      accountId: AccountId,
      context: ActorContext[Command],
  ): (BalanceState, Command) => Effect[Event, BalanceState] = (state, command) => {
    command match
      case AccountActor.Deposit(amount, replyTo) =>
        if (amount <= 0) {
          val reason = "入金額は正の数でなければなりません。"
          context.log.warn(s"[$accountId] 入金失敗: $amount. 理由: $reason")
          Effect.none.thenRun(_ => replyTo ! OperationFailed(reason))
        } else {
          context.log.info(s"[$accountId] $amount 円入金しました。新残高: ${state.currentBalance.balance} 円")
          Effect
            .persist(AccountActor.Deposited(amount))
            .thenRun(state => replyTo ! OperationSucceeded(state.currentBalance.balance))
        }
      case AccountActor.Withdraw(amount, replyTo) =>
        if (amount <= 0) {
          val reason = "出金額は正の数でなければなりません。"
          context.log.warn(s"[$accountId] 出金失敗: $amount. 理由: $reason")
          Effect.none.thenRun(_ => replyTo ! OperationFailed(reason))
        } else if (state.currentBalance.balance < amount) {
          val reason = s"残高が不足しています。残高: ${state.currentBalance.balance} 円, 出金額: $amount 円"
          context.log.warn(s"[$accountId] 出金失敗: $amount. 理由: $reason")
          Effect.none.thenRun(_ => replyTo ! OperationFailed(reason))
        } else {
          context.log.info(s"[$accountId] $amount 円出金しました。新残高: ${state.currentBalance.balance} 円")
          Effect
            .persist(AccountActor.Withdrew(amount))
            .thenRun(state => replyTo ! OperationSucceeded(state.currentBalance.balance))
        }
      case GetBalance(replyTo) =>
        context.log.info(s"[$accountId] 残高照会。現在の残高: ${state.currentBalance.balance} 円")
        Effect
          .persist(GotBalance())
          .thenRun(state => replyTo ! CurrentBalance(state.currentBalance.balance))
  }

  private val eventHandler: (BalanceState, Event) => BalanceState = { (state, event) =>
    event match {
      case Deposited(amount) => state.copy(state.history :+ Balance(amount))
      case Withdrew(amount)  => state.copy(state.history :+ Balance(-amount))
      case GotBalance()      => state
    }
  }

  object BalanceState {
    def empty: BalanceState = BalanceState(Nil)
  }
}
