package bank.repository

import bank.domain.account.AccountId
import bank.domain.transaction.Transaction
import bank.dto.TransactionDTO
import bank.util.db.DBIO
import zio.ZIO

import java.util.UUID

sealed abstract class TransactionRepositoryError(cause: Throwable) extends Throwable(cause)

case object TransactionNotFoundError        extends TransactionRepositoryError(new Error("Transaction not found"))
case class TransactionDBError(e: Throwable) extends TransactionRepositoryError(e)
case class TransactionConstructorError(message: String)
    extends TransactionRepositoryError(new RuntimeException(message))

trait TransactionRepository {
  def findById(transactionId: UUID): DBIO[TransactionRepositoryError, Transaction]
  def findByAccount(
      accountId: AccountId,
      limit: Int,
  ): DBIO[TransactionRepositoryError, List[Transaction]]
  def create(transaction: Transaction): DBIO[TransactionRepositoryError, Unit]
}

object TransactionRepositoryImpl extends TransactionRepository {

  import doobie._
  import doobie.implicits._
  import doobie.generic.auto._
  import doobie.postgres._
  import doobie.postgres.implicits._

  def findById(transactionId: UUID): DBIO[TransactionRepositoryError, Transaction] = {
    val id = transactionId.toString

    for {
      maybeTransaction <- DBIO.withDoobieSucceed {
        sql"""
          select id::text, txn_type::text, memo, created_at
          from transactions
          where id = uuid($id)
        """.query[TransactionDTO].option
      }
      transactionDTO <- ZIO
        .fromOption(maybeTransaction)
        .mapError(_ => TransactionNotFoundError)
    } yield Transaction.fromDTO(transactionDTO)
  }

  def findByAccount(
      accountId: AccountId,
      limit: Int,
  ): DBIO[TransactionRepositoryError, List[Transaction]] = {
    val accountIdString = accountId.asString

    for {
      transactionDTOs <- DBIO.withDoobieSucceed {
        sql"""
          select distinct t.id::text, t.txn_type::text, t.memo, t.created_at
          from transactions t
          join ledger_entry le on le.transaction_id = t.id
          where le.account_id = uuid($accountIdString)
          order by t.created_at desc
          limit $limit
        """.query[TransactionDTO].to[List]
      }
    } yield transactionDTOs.map(Transaction.fromDTO)
  }

  def create(transaction: Transaction): DBIO[TransactionRepositoryError, Unit] = {
    val id                    = transaction.id.toString
    val txnType               = transaction.txnType.asString
    val memo                  = transaction.memo
    val createdAt             = transaction.createdAt
    val transactionIdForEntry = transaction.id.toString

    val insertTransaction = DBIO
      .withDoobie {
        sql"""
          insert into transactions (id, txn_type, memo, created_at)
          values (uuid($id), $txnType::transaction_type, $memo, $createdAt)
        """.update.run
      }
      .mapError(TransactionDBError(_))

    val insertLedgerEntries = ZIO.foreachDiscard(transaction.entries) { entry =>
      val entryId   = entry.id.toString
      val accountId = entry.accountId.asString
      val direction = entry.direction.asString
      val amount    = entry.amount
      val postedAt  = entry.postedAt

      DBIO
        .withDoobie {
          sql"""
            insert into ledger_entry (id, account_id, transaction_id, entry_direction, amount, posted_at)
            values (
              uuid($entryId),
              uuid($accountId),
              uuid($transactionIdForEntry),
              $direction::ledger_entry_direction,
              $amount,
              $postedAt
            )
          """.update.run
        }
        .mapError(TransactionDBError(_))
    }

    insertTransaction *> insertLedgerEntries
  }
}
