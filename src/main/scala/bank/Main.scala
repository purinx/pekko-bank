package bank

import org.apache.pekko
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import bank.actor.BankGuardian

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  // private lazy val config = ConfigFactory.load("application.conf")
  // lazy val dbXa           = Transactor.fromDriverManager[IO](
  //   driver = config.getString("db.driver"),
  //   url = config.getString("db.url"),
  //   user = config.getString("db.user"),
  //   password = config.getString("user.password"),
  //   logHandler = None,
  // )

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
