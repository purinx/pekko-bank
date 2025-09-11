package bank.dto

import bank.domain.account.Account

case class AccountDTO(
    accountId: String,
    accountNumber: String,
    holderName: String,
    balance: BigDecimal,
    createdAt: String,
    updatedAt: String,
)

object AccountDTO {
  def from(account: Account): AccountDTO = {
    AccountDTO(
      accountId = account.accountId.asString,
      accountNumber = account.accountNumber,
      holderName = account.holderName,
      balance = account.balance,
      createdAt = account.createdAt.toString,
      updatedAt = account.updatedAt.toString,
    )
  }
}
