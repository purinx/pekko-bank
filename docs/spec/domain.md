# ドメインモデル

## モデル概要
- 口座（Account）と、追記専用の台帳仕訳（LedgerEntry）で残高を管理。
- 入金/出金/振替は取引（Transaction）として表現し、1..N の仕訳で構成。
- 残高は「仕訳の累計」で導出（計算可能データ）。

```mermaid
classDiagram
  class Account {
    +UUID id
    +String ownerName
    +String currency = "JPY"
    +Status status
    +Long version
    +Instant createdAt
  }
  class Transaction {
    +UUID id
    +TxnType type
    +String? idempotencyKey
    +Instant createdAt
  }
  class LedgerEntry {
    +UUID id
    +UUID accountId
    +UUID transactionId
    +Direction direction  // DEBIT or CREDIT
    +Long amountMinor     // 円（最小単位）
    +Instant postedAt
  }
  class Status { 
    ACTIVE 
    FROZEN
    CLOSED
  }
  class TxnType { 
    DEPOSIT
    WITHDRAWAL
    TRANSFER
  }
  class Direction { 
    DEBIT
    CREDIT
  }

  Account "1" <.. "*" LedgerEntry : posts
  Transaction "1" <.. "*" LedgerEntry : contains
```

- 残高計算: `balance = sum(CREDIT) - sum(DEBIT)`（口座単位）。
- 取引の冪等性: `Transaction.idempotencyKey` をユニーク制約（type+key）で担保。
- 変更履歴: 仕訳は不変。訂正は逆仕訳で取り消す（将来対応）。

## 不変条件（Invariant）
- 仕訳は一度登録したら変更不可（Immutable）。
- 振替は最低 2 行の仕訳（出金=DEBIT、入金=CREDIT）から成り、同一取引に属し原子的に登録。
- 出金/振替において `残高 >= 金額` を満たさない場合は拒否。
- 口座が `FROZEN/CLOSED` の場合、変更系取引は拒否。

## 振替フロー（概念）
```mermaid
sequenceDiagram
  participant C as Client
  participant API as API v1
  participant L as Ledger Store

  C->>API: POST /transfers (Idempotency-Key)
  API->>API: Validate + Load Accounts
  API->>L: Begin Tx (ACID)
  API->>L: Insert Transaction
  API->>L: Insert LedgerEntry (DEBIT: from)
  API->>L: Insert LedgerEntry (CREDIT: to)
  API->>L: Commit Tx
  API-->>C: 201 Created (transferId, balances)
```
