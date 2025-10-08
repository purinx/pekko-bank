package bank

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import bank.domain.account.Account

case class CreateAccountRequest(ownerName: String)

case class ListAccountResponseItem(id: String, ownerName: String)

object ListAccountResponseItem {
  def fromAccount(account: Account) = {
    ListAccountResponseItem(account.id.toString, account.ownerName)
  }
}

trait BankJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createAccountRequestFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat1(
    CreateAccountRequest.apply,
  )

  implicit val listAccountResponseItemFormat: RootJsonFormat[ListAccountResponseItem] = jsonFormat2(
    ListAccountResponseItem.apply,
  )
}
