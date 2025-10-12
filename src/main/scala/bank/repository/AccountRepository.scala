package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.dto.AccountDTO
import bank.util.db.DBIO
import zio.ZIO

sealed abstract class AccountRepositoryError(cause: Throwable) extends Throwable(cause)

final case class ConstructorError(message: String) extends AccountRepositoryError(new RuntimeException(message))

case object NotFoundError extends AccountRepositoryError(new Error("Record Not Found"))

case class DBError(e: Throwable) extends AccountRepositoryError(e)

trait AccountRepository {
  def findBy(accountId: AccountId): DBIO[AccountRepositoryError, Account]
  def create(account: Account): DBIO[AccountRepositoryError, Int]
  def all(): DBIO[AccountRepositoryError, List[Account]]
  // def update(account: Account): Result[Done]
}

object AccountRepositoryImpl extends AccountRepository {

  import doobie._
  import doobie.implicits._
  import doobie.generic.auto._
  import doobie.postgres._
  import doobie.postgres.implicits._

  def findBy(accountId: AccountId): DBIO[AccountRepositoryError, Account] =
    for {
      maybeAccountDTO <- DBIO.withDoobieSucceed {
        val id = accountId.value.toString

        sql"""
          select id, owner_name,  currency, status, version, created_at from accounts where id = $id
        """
          .query[AccountDTO]
          .option
      }
      account <- ZIO
        .fromOption(maybeAccountDTO)
        .mapError(_ => NotFoundError)
        .map(Account.fromDTO(_))
    } yield account

  def create(account: Account): DBIO[AccountRepositoryError, Int] = {
    val id        = account.id.asString
    val ownerName = account.ownerName
    val currency  = account.currency.code
    val status    = account.status.toString
    val version   = account.version

    for {
      count <- DBIO
        .withDoobie {
          sql"""
          insert into accounts (id, owner_name, currency, status, version)
          values(uuid($id), $ownerName, $currency, account_status($status), $version)
          """.update.run
        }
        .mapError(DBError(_))
    } yield count
  }

  def all(): DBIO[AccountRepositoryError, List[Account]] = {
    DBIO
      .withDoobieSucceed {
        sql"""
        select id, owner_name, currency, status, version, created_at from accounts
        """.query[AccountDTO].to[List]
      }
      .map(records => records.map(Account.fromDTO))
  }
}
