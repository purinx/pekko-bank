package bank.domain.account

import java.time.Instant
import java.util.UUID

/** Value Object: AccountId (UUID wrapper)
  */
case class AccountId(value: UUID) {
  def asString: String = value.toString
}
object AccountId {
  def newId(): AccountId                          = AccountId(UUID.randomUUID())
  def from(uuid: UUID): AccountId                 = AccountId(uuid)
  def parse(s: String): Either[String, AccountId] = {
    try {
      Right(AccountId(UUID.fromString(s)))
    } catch {
      case _: IllegalArgumentException => Left(s"Invalid UUID: '$s'")
    }
  }
}

/** Account domain model
  */
case class Account(
    accountId: AccountId,
    accountNumber: String,
    holderName: String,
    balance: BigDecimal,
    createdAt: Instant,
    updatedAt: Instant,
)

object Account {

  /** Create a new account */
  def create(accountNumber: String, holderName: String, balance: BigDecimal): Account = {
    val now = Instant.now()
    Account(
      accountId = AccountId.newId(),
      accountNumber = accountNumber,
      holderName = holderName,
      balance = balance,
      createdAt = now,
      updatedAt = now,
    )
  }
}
