package bank.actor

import bank.domain.account.AccountId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.PersistenceId

object BankGuardian {
  sealed trait Event
  final case class Deposited(amount: Long) extends Event
  final case class Withdrew(amount: Long)  extends Event
  final case class GotBalance()            extends Event

  sealed trait Command
  final case class Deliver(command: AccountActor.Command, to: String)                     extends Command
  private final case class AccountOperationResult(response: AccountActor.OperationResult) extends Command
  private final case class AccountBalanceResponse(response: AccountActor.CurrentBalance)  extends Command

  private val commandHandler: (
      AccountActor.BalanceState,
      AccountActor.Command,
  ) => Effect[Event, AccountActor.BalanceState] = { (state, command) =>
    command match
      case AccountActor.Deposit(amount, replyTo) =>
        if (amount <= 0) {
          val reason = "入金額は正の数でなければなりません。"
          Effect.none.thenRun(_ => replyTo ! AccountActor.OperationFailed(reason))
        } else {
          Effect
            .persist(Deposited(amount))
            .thenRun(state => replyTo ! AccountActor.OperationSucceeded(state.currentBalance.balance))
        }
      case AccountActor.Withdraw(amount, replyTo) =>
        if (amount <= 0) {
          val reason = "出金額は正の数でなければなりません。"
          Effect.none.thenRun(_ => replyTo ! AccountActor.OperationFailed(reason))
        } else if (state.currentBalance.balance < amount) {
          val reason = s"残高が不足しています。残高: ${state.currentBalance.balance} 円, 出金額: $amount 円"
          Effect.none.thenRun(_ => replyTo ! AccountActor.OperationFailed(reason))
        } else {
          Effect
            .persist(Withdrew(amount))
            .thenRun(state => replyTo ! AccountActor.OperationSucceeded(state.currentBalance.balance))
        }
      case AccountActor.GetBalance(replyTo) =>
        Effect
          .persist(GotBalance())
          .thenRun(state => replyTo ! AccountActor.CurrentBalance(state.currentBalance.balance))
  }

  private val eventHandler: (AccountActor.BalanceState, Event) => AccountActor.BalanceState = { (state, event) =>
    event match {
      case Deposited(amount) => state.copy(state.history :+ AccountActor.Balance(amount))
      case Withdrew(amount)  => state.copy(state.history :+ AccountActor.Balance(-amount))
      case GotBalance()      => state
    }
  }

  def applyEventSourcedBehavior(accountId: AccountId): Behavior[AccountActor.Command] =
    EventSourcedBehavior[AccountActor.Command, Event, AccountActor.BalanceState](
      persistenceId = PersistenceId.ofUniqueId(accountId.asString),
      emptyState = AccountActor.BalanceState.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler,
    )

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
        case AccountOperationResult(response) =>
          response match {
            case AccountActor.OperationSucceeded(newBalance) =>
              println(s"✅ 操作成功。新残高: $newBalance 円")
            case AccountActor.OperationFailed(reason) =>
              println(s"❌ 操作失敗: $reason")
          }
          Behaviors.same
        case AccountBalanceResponse(response) =>
          println(s"💰 現在の残高: ${response.balance} 円")
          Behaviors.same
      }
    }
  }
}
