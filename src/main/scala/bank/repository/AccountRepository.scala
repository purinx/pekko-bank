package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.dto.AccountDTO
import bank.util.db.DBIO
import zio.ZIO
import doobie._
import doobie.implicits._
import cats.effect.IO
import org.apache.pekko.Done

sealed trait AccountRepositoryError

final case class ConstructorError(message: String) extends AccountRepositoryError

case object NotFoundError extends AccountRepositoryError

case object DBError extends AccountRepositoryError

trait AccountRepository {
  def findBy(accountId: AccountId): DBIO[AccountRepositoryError, Account]
  def create(account: Account): DBIO[AccountRepositoryError, Done]
  // def update(account: Account): Result[Done]
}

object AccountRepositoryImpl extends AccountRepository {

  import doobie._
  import doobie.implicits._
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
        .flatMap(dto => ZIO.fromEither(Account.fromDTO(dto)).mapError(message => ConstructorError(message)))
    } yield account

  def create(account: Account): DBIO[AccountRepositoryError, Done] = {
    val id         = account.id.asString
    val ownerName  = account.ownerName
    val currency   = account.currency.code
    val status     = account.status.toString
    val version    = account.version
    val created_at = account.createdAt.toString()

    for {
      count <- DBIO
        .withDoobie {
          sql"""
        insert into accounts (id, oenwe_name, currency, status, version, created_at)
          values($id, $ownerName, $currency, $status, $version, $created_at)
        """.update.run
        }
        .mapError(_ => DBError)
      done <- if (count > 0) ZIO.succeed(Done) else ZIO.fail(DBError)
    } yield done
  }
}
