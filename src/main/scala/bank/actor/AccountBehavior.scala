package bank.actor

import bank.domain.account.AccountId
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import bank.domain.account.Account

object AccountBehavior {
  sealed trait Command
  final case class Create(ownerName: String, replyTo: ActorRef[OperationResult]) extends Command
  final case class Deposit(amount: Long, replyTo: ActorRef[OperationResult])     extends Command
  final case class Withdraw(amount: Long, replyTo: ActorRef[OperationResult])    extends Command

  sealed trait Event
  final case class Created(account: Account) extends Event
  final case class Deposited(amount: Long)   extends Event
  final case class Withdrawn(amount: Long)   extends Event

  sealed trait OperationResult
  final case class AccountCreated(account: Account)        extends OperationResult
  final case class OperationSucceeded(balance: BigDecimal) extends OperationResult
  final case class OperationFailed(reason: String)         extends OperationResult

  sealed trait State
  case object EmptyState                                               extends State
  final case class CreatedState(account: Account, balance: BigDecimal) extends State

  def apply(accountId: AccountId): Behavior[Command] = EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId.ofUniqueId(accountId.asString),
    emptyState = EmptyState,
    commandHandler = commandHandler(accountId),
    eventHandler = eventHandler,
  )

  private def commandHandler(id: AccountId): (state: State, command: Command) => Effect[Event, State] = {
    (state, command) =>
      command match {
        case Create(ownerName, replyTo) =>
          state match {
            case CreatedState(_, _) => Effect.none.thenReply(replyTo)(_ => OperationFailed("Account already created"))
            case EmptyState         => {
              val account = Account.create(id, ownerName)
              Effect.persist(Created(account)).thenReply(replyTo)(_ => AccountCreated(account))
            }
          }
        case Withdraw(amount, replyTo) =>
          state match {
            case CreatedState(_, balance) =>
              Effect.persist(Withdrawn(amount)).thenReply(replyTo)(_ => OperationSucceeded(balance + amount))
            case EmptyState =>
              Effect.none.thenReply(replyTo)(_ => OperationFailed("Account yet created"))
          }
        case Deposit(amount, replyTo) =>
          state match {
            case CreatedState(_, balance) =>
              if (amount >= balance)
                Effect.persist(Deposited(amount)).thenReply(replyTo)(_ => OperationSucceeded(balance - amount))
              else Effect.none.thenReply(replyTo)(_ => OperationFailed("Insufficient balance"))
            case EmptyState =>
              Effect.none.thenReply(replyTo)(_ => OperationFailed("Account yet created"))
          }
      }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    state match {
      case CreatedState(account, balance) =>
        event match {
          case Deposited(amount) => CreatedState(account, balance + amount)
          case Withdrawn(amount) => CreatedState(account, balance - amount)
          case _                 => state
        }
      case EmptyState =>
        event match {
          case Created(account) => CreatedState(account, 0)
          case _                => EmptyState
        }
    }
  }
}
