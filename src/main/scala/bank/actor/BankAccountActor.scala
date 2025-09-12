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

/** 口座アクター 入出金などの処理を担うActor
  */
object BankAccountActor {

  // --- BankAccountアクターが受け取るコマンド（メッセージ） ---
  sealed trait Command
  final case class Deposit(amount: Long, replyTo: ActorRef[OperationResult])  extends Command
  final case class Withdraw(amount: Long, replyTo: ActorRef[OperationResult]) extends Command
  final case class GetBalance(replyTo: ActorRef[CurrentBalance])              extends Command

  // --- BankAccountアクターが返信するメッセージ ---
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
}
