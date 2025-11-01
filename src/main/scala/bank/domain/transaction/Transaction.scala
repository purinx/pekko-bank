package bank.domain.transaction

import bank.dto.{DepositDTO, TransactionDTO, TransactionDetailDTO, TransferDTO, WithdrawDTO}

import java.time.Instant
import java.util.UUID

final case class TransactionId(value: UUID) {
  def asUUID: UUID    = value
  def asString: String = value.toString
}

object TransactionId {
  def newId(): TransactionId          = TransactionId(UUID.randomUUID())
  def from(uuid: UUID): TransactionId = TransactionId(uuid)
  def parse(value: String): TransactionId = TransactionId(UUID.fromString(value))
}

enum TransactionType {
  case DEPOSIT, WITHDRAWAL, TRANSFER

  def asString: String = this match
    case TransactionType.DEPOSIT    => "DEPOSIT"
    case TransactionType.WITHDRAWAL => "WITHDRAWAL"
    case TransactionType.TRANSFER   => "TRANSFER"
}

object TransactionType {
  def parse(value: String): TransactionType = {
    value.toUpperCase match
      case "DEPOSIT"    => TransactionType.DEPOSIT
      case "WITHDRAWAL" => TransactionType.WITHDRAWAL
      case "TRANSFER"   => TransactionType.TRANSFER
      case other        => throw new IllegalArgumentException(s"Unknown transaction type: $other")
  }
}

sealed trait Transaction {
  def id: TransactionId
  def memo: Option[String]
  def createdAt: Instant
  def txnType: TransactionType
}

final case class Deposit(
    id: TransactionId,
    memo: Option[String],
    createdAt: Instant,
) extends Transaction {
  override val txnType: TransactionType = TransactionType.DEPOSIT
}

final case class Withdraw(
    id: TransactionId,
    memo: Option[String],
    createdAt: Instant,
) extends Transaction {
  override val txnType: TransactionType = TransactionType.WITHDRAWAL
}

final case class Transfer(
    id: TransactionId,
    memo: Option[String],
    createdAt: Instant,
) extends Transaction {
  override val txnType: TransactionType = TransactionType.TRANSFER
}

object Transaction {
  def fromDTO(dto: TransactionDTO): Transaction =
    fromDetailDTO(TransactionDetailDTO.from(dto))

  def fromDetailDTO(dto: TransactionDetailDTO): Transaction = {
    dto match
      case DepositDTO(id, memo, createdAt) =>
        Deposit(TransactionId.parse(id), memo, createdAt)
      case WithdrawDTO(id, memo, createdAt) =>
        Withdraw(TransactionId.parse(id), memo, createdAt)
      case TransferDTO(id, memo, createdAt) =>
        Transfer(TransactionId.parse(id), memo, createdAt)
  }
}
