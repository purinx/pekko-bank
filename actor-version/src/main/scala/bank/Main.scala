package bank

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import bank.actor.Guardian

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Guardian.Command] = ActorSystem(Guardian(), "pekko-bank-actor")
    implicit val executionContext: ExecutionContext    = system.executionContext

    val accountRoutes = new AccountRoutes(system)

    // Start HTTP server
    val bindingFuture = Http().newServerAt("0.0.0.0", 8081).bind(accountRoutes.routes)

    println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
