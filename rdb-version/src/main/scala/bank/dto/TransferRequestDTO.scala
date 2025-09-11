package bank.dto

case class TransferRequestDTO(
    sourceAccountNumber: String,
    destinationAccountNumber: String,
    amount: BigDecimal,
)
