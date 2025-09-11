package bank

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import bank.repository.{AccountRepositoryImpl, TransactionRepositoryImpl}
import bank.service.TransferServiceImpl
import bank.util.db.DBIORunner
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zio.Runtime

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing]       = ActorSystem(Behaviors.empty, "pekko-bank-rdb")
    implicit val executionContext: ExecutionContext = system.executionContext

    val config = ConfigFactory.load()

    // Setup DataSource
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(config.getString("db.url"))
    hikariConfig.setUsername(config.getString("db.user"))
    hikariConfig.setPassword(config.getString("db.password"))
    hikariConfig.setDriverClassName(config.getString("db.driver"))
    val dataSource = new HikariDataSource(hikariConfig)

    // ZIO Runtime
    val zioRuntime = Runtime.default

    // Initialize components
    val dBIORunner            = new DBIORunner(dataSource, zioRuntime)
    val accountRepository     = AccountRepositoryImpl
    val transactionRepository = TransactionRepositoryImpl
    val transferService       = new TransferServiceImpl(accountRepository, transactionRepository)
    val accountRoutes         = new AccountRoutes(transferService, dBIORunner)

    // Start HTTP server
    val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(accountRoutes.routes)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ =>
        dataSource.close()
        system.terminate()
      }
  }
}
