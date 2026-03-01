-- Ledger core schema (accounts + transaction group + entries)
-- PostgreSQL

-- 1) Accounts
create table if not exists account (
  id            bigserial primary key,
  name          varchar(120) not null,
  type          varchar(30)  not null,
  active        boolean      not null default true,
  created_at    timestamptz  not null default now(),
  updated_at    timestamptz  not null default now(),

  constraint ck_account_type
    check (type in ('CHECKING','SAVINGS','CREDIT_CARD','CASH'))
);

create index if not exists ix_account_active on account(active);


-- 2) Ledger transaction group (a logical transaction that can have 1..N entries)
-- e.g. an expense will have 1 entry; a transfer will have 2 entries (debit+credit)
create table if not exists ledger_txn (
  id            bigserial primary key,
  txn_date      date         not null,
  description   varchar(500) null,
  created_at    timestamptz  not null default now()
);

create index if not exists ix_ledger_txn_txn_date on ledger_txn(txn_date);


-- 3) Ledger entries (the actual lines that affect accounts)
create table if not exists ledger_entry (
  id             bigserial primary key,
  ledger_txn_id  bigint       not null,
  account_id     bigint       not null,

  -- Always positive. Direction defines sign.
  amount         numeric(19,2) not null,
  direction      varchar(6)    not null, -- DEBIT or CREDIT

  memo           varchar(500)  null,
  created_at     timestamptz   not null default now(),

  constraint fk_entry_txn
    foreign key (ledger_txn_id) references ledger_txn(id) on delete cascade,

  constraint fk_entry_account
    foreign key (account_id) references account(id),

  constraint ck_entry_amount_positive
    check (amount > 0),

  constraint ck_entry_direction
    check (direction in ('DEBIT','CREDIT'))
);

create index if not exists ix_entry_txn_id on ledger_entry(ledger_txn_id);
create index if not exists ix_entry_account_id on ledger_entry(account_id);

-- Common query pattern: balance by account in a date range
create index if not exists ix_entry_account_txn on ledger_entry(account_id, ledger_txn_id);