package bank.service

import bank.dto.TransferRequestDTO
import bank.util.db.DBIO

sealed trait TransferServiceError
case class ValidationError(message: String)         extends TransferServiceError
case class InsufficientFunds(accountNumber: String) extends TransferServiceError
case class AccountNotFound(accountNumber: String)   extends TransferServiceError
case object SameAccountError                        extends TransferServiceError
case class TransferFailed(cause: String)            extends TransferServiceError

trait TransferService {
  def transfer(request: TransferRequestDTO): DBIO[TransferServiceError, Unit]
}
