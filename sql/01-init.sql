-- Create application database
CREATE DATABASE pekko_bank ENCODING 'UTF8';

-- Connect to the application database
\connect pekko_bank

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

-- Domain enums
DO $$ BEGIN
  CREATE TYPE account_status AS ENUM ('ACTIVE', 'FROZEN', 'CLOSED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE txn_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE entry_direction AS ENUM ('DEBIT', 'CREDIT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Accounts
CREATE TABLE IF NOT EXISTS accounts (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_name  text NOT NULL,
  currency    char(3) NOT NULL DEFAULT 'JPY',
  status      account_status NOT NULL DEFAULT 'ACTIVE',
  version     bigint NOT NULL DEFAULT 0,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT accounts_currency_jpy_only CHECK (currency = 'JPY')
);

-- Transactions (logical unit)
CREATE TABLE IF NOT EXISTS transactions (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  type             txn_type NOT NULL,
  idempotency_key  varchar(128), -- unique with type; nullable for non-idempotent ops/tests
  memo             text,
  created_at       timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT transactions_idem_unique UNIQUE (type, idempotency_key)
);

-- Ledger entries (append-only)
CREATE TABLE IF NOT EXISTS ledger_entries (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id      uuid NOT NULL REFERENCES accounts(id),
  transaction_id  uuid NOT NULL REFERENCES transactions(id),
  direction       entry_direction NOT NULL,
  amount_minor    bigint NOT NULL CHECK (amount_minor > 0),
  posted_at       timestamptz NOT NULL DEFAULT now()
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_posted_at
  ON ledger_entries (account_id, posted_at DESC, id);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_txn
  ON ledger_entries (transaction_id);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_direction
  ON ledger_entries (account_id, direction);

-- Balance view (derived from ledger)
CREATE OR REPLACE VIEW account_balances AS
SELECT
  a.id AS account_id,
  COALESCE(SUM(
    CASE le.direction
      WHEN 'CREDIT'::entry_direction THEN le.amount_minor
      WHEN 'DEBIT'::entry_direction  THEN -le.amount_minor
    END
  ), 0) AS balance
FROM accounts a
LEFT JOIN ledger_entries le ON le.account_id = a.id
GROUP BY a.id;

-- Enforce immutability on ledger_entries (no UPDATE/DELETE)
CREATE OR REPLACE FUNCTION prevent_ledger_entries_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'ledger_entries are immutable (append-only)';
END; $$;

DROP TRIGGER IF EXISTS ledger_entries_no_update ON ledger_entries;
CREATE TRIGGER ledger_entries_no_update
BEFORE UPDATE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_entries_mutation();

DROP TRIGGER IF EXISTS ledger_entries_no_delete ON ledger_entries;
CREATE TRIGGER ledger_entries_no_delete
BEFORE DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_entries_mutation();

-- Optional: simple sanity row locks via version bump (application-controlled)
-- UPDATE accounts SET version = version + 1 WHERE id = ?; -- to acquire row lock during posting

