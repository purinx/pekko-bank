package bank.dto

import java.time.Instant

final case class TransactionDTO(
    id: String,
    txnType: String,
    memo: Option[String],
    createdAt: Instant,
)

sealed trait TransactionDetailDTO {
  def id: String
  def memo: Option[String]
  def createdAt: Instant
  def txnType: String
}

final case class DepositDTO(
    id: String,
    memo: Option[String],
    createdAt: Instant,
) extends TransactionDetailDTO {
  val txnType: String = "DEPOSIT"
}

final case class WithdrawDTO(
    id: String,
    memo: Option[String],
    createdAt: Instant,
) extends TransactionDetailDTO {
  val txnType: String = "WITHDRAWAL"
}

final case class TransferDTO(
    id: String,
    memo: Option[String],
    createdAt: Instant,
) extends TransactionDetailDTO {
  val txnType: String = "TRANSFER"
}

object TransactionDetailDTO {
  def from(dto: TransactionDTO): TransactionDetailDTO = {
    dto.txnType.toUpperCase match
      case "DEPOSIT"    => DepositDTO(dto.id, dto.memo, dto.createdAt)
      case "WITHDRAWAL" => WithdrawDTO(dto.id, dto.memo, dto.createdAt)
      case "TRANSFER"   => TransferDTO(dto.id, dto.memo, dto.createdAt)
      case other        => throw new IllegalArgumentException(s"Unknown transaction type: $other")
  }
}

final case class LedgerEntryDTO(
    id: String,
    accountId: String,
    transactionId: String,
    direction: String,
    amount: Long,
    postedAt: Instant,
)
