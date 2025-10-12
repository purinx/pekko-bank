package bank.domain.account

import java.time.Instant
import java.util.UUID
import bank.dto.AccountDTO

/** Value Object: AccountId (UUID wrapper)
  */
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

/** Value Object: AccountStatus
  */
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

/** Value Object: Currency (MVP: JPY only)
  */
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

/** Account domain model (JPY-only for MVP). Balance is derived from ledger entries, not stored here.
  */
case class Account(
    id: AccountId,
    ownerName: String,
    currency: Currency,
    status: AccountStatus,
    version: Long,
    createdAt: Instant,
) {
  def isActive: Boolean = status.isActive

  /** Posting (deposit/withdraw/transfer) allowed only when ACTIVE */
  def canPostEntries: Boolean = isActive
}

object Account {
  val DefaultCurrency: Currency = Currency.JPY

  /** Create a new ACTIVE account with zero version */
  def create(ownerName: String): Account = {
    Account(
      id = AccountId.newId(),
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
