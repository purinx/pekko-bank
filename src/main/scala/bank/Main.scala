package bank

import org.apache.pekko
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import bank.actor.BankGuardian

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Main {
  def main(args: Array[String]): Unit = {
    val system           = ActorSystem(BankGuardian(), "pekko-bank")
    given ActorSystem[?] = system

    val httpRoutes = new BankRoutes(system).routes
    Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(httpRoutes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    println("http://localhost:8080")

    sys.addShutdownHook(new Thread {
      override def run = {
        system.terminate()
      }
    })

    // アクターシステムが完全に終了するまでメインスレッドを待機させる
    Await.result(system.whenTerminated, Duration.Inf): Unit
  }
}
