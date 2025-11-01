package bank.domain.ledger

import bank.domain.account.Account
import bank.domain.transaction.Transaction
import bank.dto.LedgerEntryDTO

import java.time.Instant
import java.util.UUID

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
    account: Account,
    transaction: Transaction,
    direction: LedgerEntryDirection,
    amount: Long,
    postedAt: Instant,
)

object LedgerEntry {
  def fromDTO(dto: LedgerEntryDTO, account: Account, transaction: Transaction): LedgerEntry = {
    LedgerEntry(
      id = UUID.fromString(dto.id),
      account = account,
      transaction = transaction,
      direction = LedgerEntryDirection.parse(dto.direction),
      amount = dto.amount,
      postedAt = dto.postedAt,
    )
  }
}
