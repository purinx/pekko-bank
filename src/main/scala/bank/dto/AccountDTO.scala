package bank.dto

import java.time.Instant

case class AccountDTO(
    id: String,
    ownerName: String,
    currency: String,
    status: String,
    version: Long,
    createdAt: String,
)
