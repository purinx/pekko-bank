package pekko.bank.domain.account

import java.time.Instant
import java.util.UUID

/**
  * Value Object: AccountId (UUID wrapper)
  */
case class AccountId(value: UUID) {
  def asString: String = value.toString
}
object AccountId {
  def newId(): AccountId = AccountId(UUID.randomUUID())
  def from(uuid: UUID): AccountId = AccountId(uuid)
  def parse(s: String): Either[String, AccountId] = {
    try {
      Right(AccountId(UUID.fromString(s)))
    } catch {
      case _: IllegalArgumentException => Left(s"Invalid UUID: '$s'")
    }
  }
}

/**
  * Value Object: AccountStatus
  */
enum AccountStatus {
  case ACTIVE, FROZEN, CLOSED

  def isActive: Boolean = this == AccountStatus.ACTIVE
}

/**
  * Value Object: Currency (MVP: JPY only)
  */
case class Currency private (code: String) {
  override def toString: String = code
}
object Currency {
  val JPY: Currency = Currency("JPY")

  def from(code: String): Either[String, Currency] = {
    val up = code.toUpperCase
    if (up == "JPY") Right(JPY) else Left(s"Unsupported currency: $code")
  }
}

/**
  * Account domain model (JPY-only for MVP).
  * Balance is derived from ledger entries, not stored here.
  */
case class Account(
    id: AccountId,
    ownerName: String,
    currency: Currency,
    status: AccountStatus,
    version: Long,
    createdAt: Instant
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
      createdAt = Instant.now()
    )
  }
}
