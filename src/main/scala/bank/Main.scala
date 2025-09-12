package bank

import cats.effect.IO
import doobie.util.transactor.Transactor
import org.apache.pekko
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.Http
import bank.actor.BankGuardian

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import com.typesafe.config.ConfigFactory

object Main {
  private lazy val config = ConfigFactory.load("application.conf")
  lazy val dbXa           = Transactor.fromDriverManager[IO](
    driver = config.getString("db.driver"),
    url = config.getString("db.url"),
    user = config.getString("db.user"),
    password = config.getString("user.password"),
    logHandler = None,
  )

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
