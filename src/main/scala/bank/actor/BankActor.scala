package bank.actor

import org.apache.pekko.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import cats.effect.IO
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import bank.repository.AccountRepository
import bank.repository.AccountRepositoryImpl
import bank.domain.account.Account
import bank.domain.account.AccountId

object BankActor {
  sealed trait Command

  final case class CreateAccount(ownerName: String, replyTo: ActorRef[OperationResult]) extends Command

  sealed trait OperationResult
  final case class CreateAccountSucceeded(id: AccountId) extends OperationResult
  final case class CreateAccountFailed(reason: String)   extends OperationResult

  private lazy val config = ConfigFactory.load("application.conf")
  lazy val dbXa           = Transactor.fromDriverManager[IO](
    driver = config.getString("db.driver"),
    url = config.getString("db.url"),
    user = config.getString("db.user"),
    password = config.getString("user.password"),
    logHandler = None,
  )

  given Transactor[IO] = dbXa

  val accountRepository: AccountRepository = AccountRepositoryImpl

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case CreateAccount(ownerName, replyTo) =>
        accountRepository.create(Account.create(ownerName))
        Behaviors.same
    }
  }
}

/** 銀行システムのすべてのアクターを司るGuardian
  */
object BankGuardian {
  sealed trait Command
  final case class CreateAccount(ownerName: String)                         extends Command
  final case class BankOperationResult(response: BankActor.OperationResult) extends Command

  sealed trait OperationResult

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val bankActor                                                       = context.spawn(BankActor(), "bank")
    val bankOperationResultAdapter: ActorRef[BankActor.OperationResult] =
      context.messageAdapter(BankOperationResult.apply(_))

    Behaviors.receiveMessage { case CreateAccount(ownerName) =>
      bankActor ! BankActor.CreateAccount(ownerName, bankOperationResultAdapter)
      Behaviors.same
    }
  }
}
