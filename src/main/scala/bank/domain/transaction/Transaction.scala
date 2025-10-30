package bank.domain.transaction

import bank.domain.account.AccountId
import bank.dto.{LedgerEntryDTO, TransactionDTO}

import java.time.Instant
import java.util.UUID

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

enum LedgerEntryDirection {
  case CREDIT, DEBIT

  def asString: String = this match
    case LedgerEntryDirection.CREDIT => "CREDIT"
    case LedgerEntryDirection.DEBIT  => "DEBIT"
}

object LedgerEntryDirection {
  def parse(value: String): LedgerEntryDirection = {
    value.toUpperCase match
      case "CREDIT" => LedgerEntryDirection.CREDIT
      case "DEBIT"  => LedgerEntryDirection.DEBIT
      case other    => throw new IllegalArgumentException(s"Unknown ledger entry direction: $other")
  }
}

final case class LedgerEntry(
    id: UUID,
    accountId: AccountId,
    transactionId: UUID,
    direction: LedgerEntryDirection,
    amount: Long,
    postedAt: Instant,
)

object LedgerEntry {
  def fromDTO(dto: LedgerEntryDTO): LedgerEntry = {
    LedgerEntry(
      id = UUID.fromString(dto.id),
      accountId = AccountId.parse(dto.accountId),
      transactionId = UUID.fromString(dto.transactionId),
      direction = LedgerEntryDirection.parse(dto.direction),
      amount = dto.amount,
      postedAt = dto.postedAt,
    )
  }
}

final case class Transaction(
    id: UUID,
    txnType: TransactionType,
    memo: Option[String],
    createdAt: Instant,
    entries: List[LedgerEntry],
) {
  def withEntries(entries: List[LedgerEntry]): Transaction = copy(entries = entries)
}

object Transaction {
  def fromDTO(dto: TransactionDTO): Transaction = {
    Transaction(
      id = UUID.fromString(dto.id),
      txnType = TransactionType.parse(dto.txnType),
      memo = dto.memo,
      createdAt = dto.createdAt,
      entries = Nil,
    )
  }

  def fromDTO(dto: TransactionDTO, entryDTOs: List[LedgerEntryDTO]): Transaction = {
    Transaction(
      id = UUID.fromString(dto.id),
      txnType = TransactionType.parse(dto.txnType),
      memo = dto.memo,
      createdAt = dto.createdAt,
      entries = entryDTOs.map(LedgerEntry.fromDTO),
    )
  }
}
