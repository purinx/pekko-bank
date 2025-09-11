package bank

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import bank.actor.{Guardian, TransferCoordinator}
import bank.dto.TransferRequestDTO
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.duration._

class AccountRoutes(guardian: ActorRef[Guardian.Command])(implicit system: ActorSystem[_]) {
  private implicit val timeout: Timeout = 5.seconds

  lazy val routes: Route =
    pathPrefix("transfer") {
      post {
        entity(as[TransferRequestDTO]) { request =>
          val result = guardian.ask[TransferCoordinator.TransferResult] { ref =>
            Guardian.ProcessTransfer(
              request.sourceAccountNumber,
              request.destinationAccountNumber,
              request.amount,
              ref,
            )
          }
          onSuccess(result) {
            case TransferCoordinator.TransferSucceeded =>
              complete(StatusCodes.OK, "Transfer successful")
            case TransferCoordinator.TransferFailed(reason) =>
              complete(StatusCodes.BadRequest, s"Transfer failed: $reason")
          }
        }
      }
    }
}
