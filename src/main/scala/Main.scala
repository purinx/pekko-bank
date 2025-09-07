import org.apache.pekko
import org.apache.pekko.http.scaladsl.Http
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Try

/** 口座アクター (BankAccount Actor) (このアクターのロジックは変更ありません)
  */
object BankAccount {

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

/** アプリケーション全体を管理するガーディアンアクター (Guardian Actor) (このアクターのロジックは変更ありません)
  */
object BankGuardian {

  sealed trait Command
  final case class ProcessLine(line: String)                                             extends Command
  final case class Deliver(command: BankAccount.Command, to: String)                     extends Command
  private final case class AccountOperationResult(response: BankAccount.OperationResult) extends Command
  private final case class AccountBalanceResponse(response: BankAccount.CurrentBalance)  extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val operationResultAdapter: ActorRef[BankAccount.OperationResult] =
        context.messageAdapter(AccountOperationResult.apply(_))
      val balanceResponseAdapter: ActorRef[BankAccount.CurrentBalance] =
        context.messageAdapter(AccountBalanceResponse.apply(_))
      val accountActor = context.spawn(BankAccount("account-1234"), "account-1234")

      Behaviors.receiveMessage {
        case ProcessLine(line) =>
          line.trim.toLowerCase.split(" ").toList match {
            case "deposit" :: amountStr :: Nil =>
              Try(amountStr.toLong).toOption.foreach(amount =>
                accountActor ! BankAccount.Deposit(amount, operationResultAdapter),
              )
            case "withdraw" :: amountStr :: Nil =>
              Try(amountStr.toLong).toOption.foreach(amount =>
                accountActor ! BankAccount.Withdraw(amount, operationResultAdapter),
              )
            case "getbalance" :: Nil =>
              accountActor ! BankAccount.GetBalance(balanceResponseAdapter)
            case "quit" :: Nil =>
              println("アプリケーションを終了します。")
              context.system.terminate()
            case _ =>
              println(s"無効なコマンドです: '$line'")
          }
          Behaviors.same
        case Deliver(command, to) => {
          val target: ActorRef[BankAccount.Command] =
            context.child(to) match {
              case Some(ref) => ref.unsafeUpcast[BankAccount.Command]
              case None      => context.spawn(BankAccount(to), to)
            }
          target ! command
          Behaviors.same
        }
        case AccountOperationResult(response) =>
          response match {
            case BankAccount.OperationSucceeded(newBalance) =>
              println(s"✅ 操作成功。新残高: $newBalance 円")
            case BankAccount.OperationFailed(reason) =>
              println(s"❌ 操作失敗: $reason")
          }
          Behaviors.same
        case AccountBalanceResponse(response) =>
          println(s"💰 現在の残高: ${response.balance} 円")
          Behaviors.same
      }
    }
}

/** アプリケーションのエントリーポイント
  */
object Main {
  def main(args: Array[String]): Unit = {
    val system           = ActorSystem(BankGuardian(), "pekko-bank")
    given ActorSystem[?] = system

    val httpRoutes = new AccountRoutes(system).routes
    Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(httpRoutes)

    println("\n--- Pekko銀行へようこそ ---")
    println("コマンドを入力してください:")
    println("  deposit <金額>   (例: deposit 10000)")
    println("  withdraw <金額>  (例: withdraw 3000)")
    println("  getbalance")
    println("  quit (終了)")
    println("-------------------------\n")

    // 'quit'が入力されるか、入力ストリームが閉じるまでコマンドを読み込みます
    var command = ""
    while ({ command = StdIn.readLine("> "); command != null && command.trim.toLowerCase != "quit" }) {
      if (command.trim.nonEmpty) {
        system ! BankGuardian.ProcessLine(command)
      }
    }

    // ループを抜けた理由が'quit'コマンドの場合、アクターに終了を伝えます
    if (command != null && command.trim.toLowerCase == "quit") {
      system ! BankGuardian.ProcessLine("quit")
    } else {
      // 入力ストリームが閉じた場合（Ctrl+Dなど）
      system.terminate()
    }

    // アクターシステムが完全に終了するまでメインスレッドを待機させます
    Await.result(system.whenTerminated, Duration.Inf): Unit
  }
}
