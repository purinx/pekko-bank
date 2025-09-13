package bank

import org.apache.pekko
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import bank.actor.{BankGuardian, AccountRepositoryActor}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import bank.repository.AccountRepositoryImpl
import bank.util.db.DBIORunner

object Main {
  def main(args: Array[String]): Unit = {
    val config       = ConfigFactory.load()
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(config.getString("db.url"))
    hikariConfig.setUsername(config.getString("db.user"))
    hikariConfig.setPassword(config.getString("db.password"))
    hikariConfig.setDriverClassName(config.getString("db.driver"))

    val dataSource        = new HikariDataSource(hikariConfig)
    val dbIORunner        = new DBIORunner(dataSource)
    val accountRepository = AccountRepositoryImpl

    val accountRepositoryBehavior = AccountRepositoryActor(accountRepository, dbIORunner)
    val bankGuardian              = BankGuardian(accountRepositoryBehavior)
    val system                    = ActorSystem(bankGuardian, "pekko-bank")
    given ActorSystem[?]          = system

    val httpRoutes = new BankRoutes(system).routes
    Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(httpRoutes)

    println("http://localhost:8080")

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
