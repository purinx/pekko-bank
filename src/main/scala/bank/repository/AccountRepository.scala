package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.dto.AccountDTO
import bank.util.db.DBIO
import zio.ZIO

sealed trait AccountRepositoryError

final case class ConstructorError(message: String) extends AccountRepositoryError

case object NotFoundError extends AccountRepositoryError

trait AccountRepository {
  def findBy(accountId: AccountId): DBIO[AccountRepositoryError, Account]
  // def create(account: Account): Result[Done]
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

}
