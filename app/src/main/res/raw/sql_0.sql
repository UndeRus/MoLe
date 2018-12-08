create table if not exists accounts(name varchar);
create index if not exists idx_accounts_name on accounts(name);
create table if not exists options(name varchar, value varchar);
create unique index if not exists idx_options_name on options(name);
create table if not exists account_values(account varchar not null, currency varchar not null, value decimal(18,2) not null);
create index if not exists idx_account_values_account on account_values(account);
create unique index if not exists un_account_values on account_values(account,currency);
