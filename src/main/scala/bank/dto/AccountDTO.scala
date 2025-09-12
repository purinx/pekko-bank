package bank.dto

case class AccountDTO(
    id: String,
    ownerName: String,
    currency: String,
    status: String,
    version: Long,
    createdAt: String,
)
