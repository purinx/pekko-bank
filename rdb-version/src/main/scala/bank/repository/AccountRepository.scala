package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.util.db.DBIO
import zio.ZIO
import doobie._
import doobie.implicits._

import doobie.postgres.implicits._
import java.time.Instant
import java.util.UUID

sealed trait AccountRepositoryError
final case class AccountRepositoryErrorImpl(message: String) extends AccountRepositoryError
case object AccountNotFoundError                             extends AccountRepositoryError

trait AccountRepository {
  def findByAccountNumber(accountNumber: String): DBIO[AccountRepositoryError, Account]
  def findByAccountNumberWithLock(accountNumber: String): DBIO[AccountRepositoryError, Account]
  def update(account: Account): DBIO[AccountRepositoryError, Unit]
}

object AccountRepositoryImpl extends AccountRepository {

  private def toAccount(
      id: UUID,
      accountNumber: String,
      holderName: String,
      balance: BigDecimal,
      createdAt: Instant,
      updatedAt: Instant,
  ): Account = {
    Account(
      accountId = AccountId(id),
      accountNumber = accountNumber,
      holderName = holderName,
      balance = balance,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
  }

  override def findByAccountNumber(accountNumber: String): DBIO[AccountRepositoryError, Account] = {
    DBIO
      .transact {
        sql"""
        SELECT account_id, account_number, holder_name, balance, created_at, updated_at
        FROM accounts
        WHERE account_number = $accountNumber
      """
          .query[(UUID, String, String, BigDecimal, Instant, Instant)]
          .map((toAccount _).tupled)
          .option
      }
      .mapError(e => AccountRepositoryErrorImpl(e.getMessage))
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundError))
  }

  override def findByAccountNumberWithLock(accountNumber: String): DBIO[AccountRepositoryError, Account] = {
    DBIO
      .transact {
        sql"""
        SELECT account_id, account_number, holder_name, balance, created_at, updated_at
        FROM accounts
        WHERE account_number = $accountNumber
        FOR UPDATE
      """
          .query[(UUID, String, String, BigDecimal, Instant, Instant)]
          .map((toAccount _).tupled)
          .option
      }
      .mapError(e => AccountRepositoryErrorImpl(e.getMessage))
      .flatMap(ZIO.fromOption(_).mapError(_ => AccountNotFoundError))
  }

  override def update(account: Account): DBIO[AccountRepositoryError, Unit] = {
    DBIO
      .transact {
        sql"""
        UPDATE accounts
        SET balance = ${account.balance}, updated_at = ${Instant.now()}
        WHERE account_id = ${account.accountId.value}
      """.update.run
      }
      .mapError(e => AccountRepositoryErrorImpl(e.getMessage))
      .unit
  }
}
