package bank.service

import bank.domain.account.Account
import bank.domain.transaction.{Transaction, TransactionType}
import bank.dto.TransferRequestDTO
import bank.repository.{AccountRepository, AccountRepositoryError, TransactionRepository, AccountNotFoundError}
import bank.util.db.DBIO
import zio.ZIO

class TransferServiceImpl(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
) extends TransferService {

  override def transfer(request: TransferRequestDTO): DBIO[TransferServiceError, Unit] = {
    val program: DBIO[TransferServiceError, Unit] = for {
      _                  <- validateRequest(request)
      sourceAccount      <- findAndLockAccount(request.sourceAccountNumber)
      destinationAccount <- findAndLockAccount(request.destinationAccountNumber)
      _                  <- checkBalance(sourceAccount, request.amount)
      updatedSource      = sourceAccount.copy(balance = sourceAccount.balance - request.amount)
      updatedDestination = destinationAccount.copy(balance = destinationAccount.balance + request.amount)
      _ <- accountRepository.update(updatedSource).mapError(e => TransferFailed(e.toString))
      _ <- accountRepository.update(updatedDestination).mapError(e => TransferFailed(e.toString))
      _ <- recordTransactions(updatedSource, updatedDestination, request.amount)
    } yield ()
    program
  }

  private def validateRequest(request: TransferRequestDTO): ZIO[Any, TransferServiceError, Unit] = {
    if (request.sourceAccountNumber == request.destinationAccountNumber) {
      ZIO.fail(SameAccountError)
    } else if (request.amount <= 0) {
      ZIO.fail(ValidationError("Amount must be positive"))
    } else {
      ZIO.unit
    }
  }

  private def findAndLockAccount(accountNumber: String): DBIO[TransferServiceError, Account] = {
    accountRepository
      .findByAccountNumberWithLock(accountNumber)
      .mapError {
        case AccountNotFoundError => AccountNotFound(accountNumber)
        case e                    => TransferFailed(s"Failed to find account: $e")
      }
  }

  private def checkBalance(account: Account, amount: BigDecimal): ZIO[Any, TransferServiceError, Unit] = {
    if (account.balance < amount) {
      ZIO.fail(InsufficientFunds(account.accountNumber))
    } else {
      ZIO.unit
    }
  }

  private def recordTransactions(
      source: Account,
      destination: Account,
      amount: BigDecimal,
  ): DBIO[TransferServiceError, Unit] = {
    val withdrawal = Transaction.create(
      accountId = source.accountId,
      transactionType = TransactionType.WITHDRAWAL,
      amount = amount,
      counterpartyAccountNumber = Some(destination.accountNumber),
    )
    val deposit = Transaction.create(
      accountId = destination.accountId,
      transactionType = TransactionType.DEPOSIT,
      amount = amount,
      counterpartyAccountNumber = Some(source.accountNumber),
    )

    transactionRepository
      .create(withdrawal)
      .zipRight(transactionRepository.create(deposit))
      .mapError(e => TransferFailed(s"Failed to record transaction: $e"))
  }
}
