package bank.actor

import org.apache.pekko
import pekko.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import cats.effect.IO
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.Behaviors
import bank.repository.AccountRepository
import bank.repository.AccountRepositoryImpl
import bank.domain.account.Account
import bank.domain.account.AccountId
import zio.Task

object Bank {
  sealed trait Command
  final case class CreateAccount(ownerName: String, replyTo: ActorRef[OperationResult]) extends Command

  sealed trait OperationResult
  final case class CreateAccountSucceeded(id: AccountId) extends OperationResult
  final case class CreateAccountFailed(reason: String)   extends OperationResult

  private lazy val config = ConfigFactory.load("application.conf")
  lazy val dbXa           = Transactor.fromDriverManager[Task](
    driver = config.getString("db.driver"),
    url = config.getString("db.url"),
    user = config.getString("db.user"),
    password = config.getString("user.password"),
    logHandler = None,
  )

  given Transactor[Task] = dbXa

  val accountRepository: AccountRepository = AccountRepositoryImpl

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case CreateAccount(ownerName, replyTo) =>

        accountRepository.create(Account.create(ownerName))
        replyTo
        Behaviors.same
    }
  }
}

/** 銀行システムのすべてのアクターを司るGuardian
  */
object BankGuardian {
  sealed trait Command
  final case class CreateAccount(ownerName: String)                extends Command
  final case class OperationResult(response: Bank.OperationResult) extends Command
  final case class Deliver(command: Bank.Command, to: String)      extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val bankActor                                                  = context.spawn(Bank(), "bank")
    val bankOperationResultAdapter: ActorRef[Bank.OperationResult] =
      context.messageAdapter(OperationResult.apply(_))

    Behaviors.receiveMessage { case CreateAccount(ownerName) =>
      bankActor ! Bank.CreateAccount(ownerName, bankOperationResultAdapter)
      Behaviors.same
    }
  }
}
