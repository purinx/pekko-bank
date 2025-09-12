package bank.domain.transaction

import bank.domain.account.AccountId
import java.time.Instant
import java.util.UUID

// TransactionId value object
case class TransactionId(value: UUID) {
  def asString: String = value.toString
}
object TransactionId {
  def newId(): TransactionId = TransactionId(UUID.randomUUID())
}

// TransactionType enum
enum TransactionType {
  case DEPOSIT, WITHDRAWAL
}

// Transaction domain model
case class Transaction(
    transactionId: TransactionId,
    accountId: AccountId,
    transactionType: TransactionType,
    amount: BigDecimal,
    counterpartyAccountNumber: Option[String], // 相手がいない取引もあるためOption
    transactionDatetime: Instant,
)

object Transaction {
  def create(
      accountId: AccountId,
      transactionType: TransactionType,
      amount: BigDecimal,
      counterpartyAccountNumber: Option[String],
  ): Transaction = {
    Transaction(
      transactionId = TransactionId.newId(),
      accountId = accountId,
      transactionType = transactionType,
      amount = amount,
      counterpartyAccountNumber = counterpartyAccountNumber,
      transactionDatetime = Instant.now(),
    )
  }
}
