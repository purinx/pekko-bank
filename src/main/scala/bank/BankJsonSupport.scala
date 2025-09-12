package bank

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

case class CreateAccountRequest(ownerName: String)

trait BankJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createAccountRequestFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat1(
    CreateAccountRequest.apply,
  )
}
