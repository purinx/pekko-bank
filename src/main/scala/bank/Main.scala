package bank

import org.apache.pekko
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import bank.actor.BankGuardian

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import bank.repository.AccountRepositoryImpl
import bank.util.db.DBIORunner

object Main {
  def main(args: Array[String]): Unit = {
    val system           = ActorSystem(BankGuardian(), "pekko-bank")
    given ActorSystem[?] = system
    val config           = ConfigFactory.load()
    val hikariConfig     = new HikariConfig()
    hikariConfig.setJdbcUrl(config.getString("db.url"))
    hikariConfig.setUsername(config.getString("db.user"))
    hikariConfig.setPassword(config.getString("db.password"))
    hikariConfig.setDriverClassName(config.getString("db.driver"))

    val dataSource             = new HikariDataSource(hikariConfig)
    val dbIORunner             = new DBIORunner(dataSource)
    lazy val accountRepository = AccountRepositoryImpl

    val httpRoutes = new BankRoutes(system, accountRepository, dbIORunner).routes
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
