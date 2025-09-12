package bank.repository

import bank.domain.transaction.Transaction
import bank.util.db.DBIO
import zio.ZIO
import doobie._
import doobie.implicits._

import doobie.postgres.implicits._
import java.time.Instant
import java.util.UUID

sealed trait TransactionRepositoryError
final case class TransactionRepositoryErrorImpl(message: String) extends TransactionRepositoryError

trait TransactionRepository {
  def create(transaction: Transaction): DBIO[TransactionRepositoryError, Unit]
}

object TransactionRepositoryImpl extends TransactionRepository {
  override def create(transaction: Transaction): DBIO[TransactionRepositoryError, Unit] = {
    DBIO
      .transact {
        sql"""
        INSERT INTO transactions (transaction_id, account_id, transaction_type, amount, counterparty_account_number, transaction_datetime)
        VALUES (
          ${transaction.transactionId.value},
          ${transaction.accountId.value},
          ${transaction.transactionType.toString},
          ${transaction.amount},
          ${transaction.counterpartyAccountNumber},
          ${transaction.transactionDatetime}
        )
      """.update.run
      }
      .mapError(e => TransactionRepositoryErrorImpl(e.getMessage))
      .unit
  }
}
