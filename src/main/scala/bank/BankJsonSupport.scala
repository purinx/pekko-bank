package bank

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

case class CreateAccountRequest(ownerName: String)

case class AccountOperationSuccessResponse(balance: BigDecimal)

case class AccountOperationFailureResponse(message: String)

trait BankJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createAccountRequestFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat1(
    CreateAccountRequest.apply,
  )

  implicit val accountOperationSuccessResponseFormat: RootJsonFormat[AccountOperationSuccessResponse] = jsonFormat1(
    AccountOperationSuccessResponse.apply,
  )

  implicit val accountOperationErrorResponseFormat: RootJsonFormat[AccountOperationFailureResponse] = jsonFormat1(
    AccountOperationFailureResponse.apply,
  )

}
