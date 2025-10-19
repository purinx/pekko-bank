package bank.domain.account

import java.time.Instant
import java.util.UUID
import bank.dto.AccountDTO

case class AccountId(value: UUID) {
  def asString: String = value.toString
}

object AccountId {
  def newId(): AccountId          = AccountId(UUID.randomUUID())
  def from(uuid: UUID): AccountId = AccountId(uuid)
  def parse(s: String)            = {
    AccountId(UUID.fromString(s))
  }
}

enum AccountStatus {
  case ACTIVE, FROZEN, CLOSED

  def isActive: Boolean = this == AccountStatus.ACTIVE
}

object AccountStatus {
  def parse(status: String): AccountStatus = {
    status match
      case "ACTIVE" => AccountStatus.ACTIVE
      case "FROZEN" => AccountStatus.FROZEN
      case "CLOSED" => AccountStatus.CLOSED
      case value    => throw new Exception(s"Unknown status $value")
  }
}

case class Currency private (code: String) {
  override def toString: String = code
}
object Currency {
  val JPY: Currency = Currency("JPY")

  def parse(code: String): Currency = {
    val up = code.toUpperCase
    if (up == "JPY") JPY else throw new Exception(s"Unsupported currency: $code")
  }
}

case class Account(
    id: AccountId,
    ownerName: String,
    currency: Currency,
    status: AccountStatus,
    version: Long,
    createdAt: Instant,
) {
  def isActive: Boolean = status.isActive
}

object Account {
  val DefaultCurrency: Currency = Currency.JPY

  def create(id: AccountId, ownerName: String): Account = {
    Account(
      id = id,
      ownerName = ownerName,
      currency = DefaultCurrency,
      status = AccountStatus.ACTIVE,
      version = 0L,
      createdAt = Instant.now(),
    )
  }

  def fromDTO(dto: AccountDTO): Account = {
    Account(
      AccountId.parse(dto.id),
      dto.ownerName,
      Currency.parse(dto.currency),
      AccountStatus.parse(dto.status),
      dto.version,
      dto.createdAt,
    )
  }
}
