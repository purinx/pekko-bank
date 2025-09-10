package bank

import org.apache.pekko
import org.apache.pekko.http.scaladsl.Http
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Try

import doobie.util.transactor.Transactor
import cats.effect.IO
import com.typesafe.config.ConfigFactory
import bank.repository.AccountRepositoryImpl
import bank.repository.AccountRepository
import bank.domain.account.Account
import bank.actor.BankGuardian

object Main {
  def main(args: Array[String]): Unit = {
    val system           = ActorSystem(BankGuardian(), "pekko-bank")
    given ActorSystem[?] = system

    val httpRoutes = new BankRoutes(system).routes
    Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(httpRoutes)

    Runtime
      .getRuntime()
      .addShutdownHook(new Thread {
        override def run = {
          system.terminate()
        }
      })

    // アクターシステムが完全に終了するまでメインスレッドを待機させます
    Await.result(system.whenTerminated, Duration.Inf): Unit
  }
}
