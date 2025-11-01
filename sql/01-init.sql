-- Create application database
CREATE DATABASE pekko_bank ENCODING 'UTF8';

-- Connect to the application database
\connect pekko_bank

CREATE TABLE IF NOT EXISTS event_journal(
  ordering BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,

  writer VARCHAR(255) NOT NULL,
  write_timestamp BIGINT,
  adapter_manifest VARCHAR(255),

  event_ser_id INTEGER NOT NULL,
  event_ser_manifest VARCHAR(255) NOT NULL,
  event_payload BYTEA NOT NULL,

  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY(persistence_id, sequence_number)
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON public.event_journal(ordering);

CREATE TABLE IF NOT EXISTS event_tag(
    event_id BIGINT,
    tag VARCHAR(256),
    PRIMARY KEY(event_id, tag),
    CONSTRAINT fk_event_journal
      FOREIGN KEY(event_id)
      REFERENCES event_journal(ordering)
      ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,

  snapshot_ser_id INTEGER NOT NULL,
  snapshot_ser_manifest VARCHAR(255) NOT NULL,
  snapshot_payload BYTEA NOT NULL,

  meta_ser_id INTEGER,
  meta_ser_manifest VARCHAR(255),
  meta_payload BYTEA,

  PRIMARY KEY(persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS durable_state (
    global_offset BIGSERIAL,
    persistence_id VARCHAR(255) NOT NULL,
    revision BIGINT NOT NULL,
    state_payload BYTEA NOT NULL,
    state_serial_id INTEGER NOT NULL,
    state_serial_manifest VARCHAR(255),
    tag VARCHAR,
    state_timestamp BIGINT NOT NULL,
    PRIMARY KEY(persistence_id)
    );
CREATE INDEX CONCURRENTLY state_tag_idx on durable_state (tag);
CREATE INDEX CONCURRENTLY state_global_offset_idx on durable_state (global_offset);

DO $$
BEGIN
  CREATE TYPE ledger_entry_direction AS ENUM ('CREDIT', 'DEBIT');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END
$$;

DO $$
BEGIN
  CREATE TYPE account_status AS ENUM ('ACTIVE', 'FROZEN', 'CLOSED');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END
$$;

CREATE TABLE IF NOT EXISTS accounts (
  id UUID PRIMARY KEY,
  owner_name TEXT NOT NULL,
  currency TEXT NOT NULL,
  status account_status NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deposits (
  id UUID PRIMARY KEY,
  memo TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS withdraws (
  id UUID PRIMARY KEY,
  memo TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transfers (
  id UUID PRIMARY KEY,
  memo TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ledger_entry (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL,
  deposit_id UUID,
  withdrawal_id UUID,
  transfer_id UUID,
  entry_direction ledger_entry_direction NOT NULL,
  amount BIGINT NOT NULL CHECK (amount >= 0),
  posted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ledger_entry_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT ledger_entry_deposit_fk FOREIGN KEY (deposit_id) REFERENCES deposits(id),
  CONSTRAINT ledger_entry_withdrawal_fk FOREIGN KEY (withdrawal_id) REFERENCES withdraws(id),
  CONSTRAINT ledger_entry_transfer_fk FOREIGN KEY (transfer_id) REFERENCES transfers(id),
  CONSTRAINT ledger_entry_single_transaction CHECK (
    (CASE WHEN deposit_id IS NOT NULL THEN 1 ELSE 0 END) +
    (CASE WHEN withdrawal_id IS NOT NULL THEN 1 ELSE 0 END) +
    (CASE WHEN transfer_id IS NOT NULL THEN 1 ELSE 0 END) = 1
  )
);

CREATE INDEX IF NOT EXISTS ledger_entry_account_id_idx ON ledger_entry (account_id);
CREATE INDEX IF NOT EXISTS ledger_entry_deposit_id_idx ON ledger_entry (deposit_id);
CREATE INDEX IF NOT EXISTS ledger_entry_withdrawal_id_idx ON ledger_entry (withdrawal_id);
CREATE INDEX IF NOT EXISTS ledger_entry_transfer_id_idx ON ledger_entry (transfer_id);
