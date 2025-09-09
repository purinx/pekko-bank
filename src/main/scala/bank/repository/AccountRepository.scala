package bank.repository

import bank.domain.account.{Account, AccountId}
import bank.dto.AccountDTO
import doobie._
import doobie.implicits._
import cats.effect.IO

sealed trait AccountRepositoryError

final case class ConstructorError(message: String) extends AccountRepositoryError

case object NotFoundError extends AccountRepositoryError

private type Result[T] = IO[Either[AccountRepositoryError, T]]

trait AccountRepository {
  def findBy(accountId: AccountId): Result[Account]
  // def create(account: Account): Result[Done]
  // def update(account: Account): Result[Done]
}

class AccountRepositoryImpl(using Transactor[IO]) extends AccountRepository {
  def findBy(accountId: AccountId): Result[Account] = {
    val id = accountId.value.toString

    sql"""
      select id, owner_name,  currency, status, version, created_at from accounts where id = $id
    """
      .query[AccountDTO]
      .option
      .transact(summon[Transactor[IO]])
      .map(_.toRight(NotFoundError))
      .map(_.flatMap(Account.fromDTO(_).left.map(ConstructorError(_))))
  }
}
