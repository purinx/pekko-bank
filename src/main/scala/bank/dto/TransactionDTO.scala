package bank.dto

import java.time.Instant

final case class TransactionDTO(
    id: String,
    txnType: String,
    memo: Option[String],
    createdAt: Instant,
)

final case class LedgerEntryDTO(
    id: String,
    accountId: String,
    transactionId: String,
    direction: String,
    amount: Long,
    postedAt: Instant,
)
