package bank

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.model.StatusCodes
import bank.dto.TransferRequestDTO
import bank.service._
import bank.util.db.DBIORunner
import scala.concurrent.ExecutionContext
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

class AccountRoutes(transferService: TransferService, dBIORunner: DBIORunner)(implicit ec: ExecutionContext) {

  lazy val routes: Route =
    pathPrefix("transfer") {
      post {
        entity(as[TransferRequestDTO]) { request =>
          val resultFuture = dBIORunner.run(transferService.transfer(request))
          onSuccess(resultFuture) {
            case Right(_) =>
              complete(StatusCodes.OK, "Transfer successful")
            case Left(error: TransferServiceError) =>
              error match {
                case ValidationError(msg)   => complete(StatusCodes.BadRequest, s"Validation error: $msg")
                case InsufficientFunds(num) => complete(StatusCodes.BadRequest, s"Insufficient funds in account: $num")
                case AccountNotFound(num)   => complete(StatusCodes.NotFound, s"Account not found: $num")
                case SameAccountError       =>
                  complete(StatusCodes.BadRequest, "Source and destination accounts cannot be the same")
                case TransferFailed(cause) => complete(StatusCodes.InternalServerError, s"Transfer failed: $cause")
              }
            case Left(otherError) =>
              complete(StatusCodes.InternalServerError, s"An unexpected error occurred: ${otherError.getMessage}")
          }
        }
      }
    }
}
