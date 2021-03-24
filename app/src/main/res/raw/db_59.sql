-- Copyright © 2021 Damyan Ivanov.
-- This file is part of MoLe.
-- MoLe is free software: you can distribute it and/or modify it
-- under the term of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your opinion), any later version.
--
-- MoLe is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
-- GNU General Public License terms for details.
--
-- You should have received a copy of the GNU General Public License
-- along with MoLe. If not, see <https://www.gnu.org/licenses/>.

-- migrate from revision 58 to revision 59

-- pragmas need to be outside of transaction control
-- foreign_keys is needed so that foreign key constraints are redirected

commit transaction;
pragma foreign_keys = on;

begin transaction;

-- profiles
CREATE TABLE profiles_new (
id INTEGER NOT NULL PRIMARY KEY,
deprecated_uuid text,
name TEXT NOT NULL,
url TEXT NOT NULL,
use_authentication INTEGER NOT NULL,
auth_user TEXT,
auth_password TEXT,
order_no INTEGER NOT NULL,
permit_posting INTEGER NOT NULL,
theme INTEGER NOT NULL DEFAULT -1,
preferred_accounts_filter TEXT,
future_dates INTEGER NOT NULL,
api_version INTEGER NOT NULL,
show_commodity_by_default INTEGER NOT NULL,
default_commodity TEXT,
show_comments_by_default INTEGER NOT NULL DEFAULT 1,
detected_version_pre_1_19 INTEGER NOT NULL,
detected_version_major INTEGER NOT NULL,
detected_version_minor INTEGER NOT NULL);

insert into profiles_new(
       deprecated_uuid, name, url, use_authentication, auth_user, auth_password,
       order_no, permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
       show_commodity_by_default, default_commodity, show_comments_by_default, detected_version_pre_1_19,
       detected_version_major, detected_version_minor)
select uuid, name, url, use_authentication, auth_user, auth_password,
       order_no, permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
       show_commodity_by_default, default_commodity, show_comments_by_default, detected_version_pre_1_19,
       detected_version_major, detected_version_minor
from profiles;

-- accounts
create table accounts_new(
id integer primary key not null,
profile_id integer not null references profiles_new(id) on delete cascade on update restrict,
level INTEGER NOT NULL,
name TEXT NOT NULL,
name_upper TEXT NOT NULL,
parent_name TEXT,
expanded INTEGER NOT NULL DEFAULT 1,
amounts_expanded INTEGER NOT NULL DEFAULT 0,
generation INTEGER NOT NULL DEFAULT 0);

insert into accounts_new(profile_id, level, name, name_upper, parent_name, expanded, amounts_expanded, generation)
select p.id, a.level, a.name, a.name_upper, a.parent_name, a.expanded, a.amounts_expanded, a.generation
from profiles_new p
join accounts a on a.profile=p.deprecated_uuid;

drop table accounts;
alter table accounts_new rename to accounts;

drop table profiles;
alter table profiles_new rename to profiles;

create index fk_account_profile on accounts(profile_id);
create unique index un_account_name on accounts(profile_id, name);

-- options
create table options_new(
name text not null,
profile_id integer not null,
value text,
primary key(profile_id,name));

insert into options_new(name, profile_id, value)
select o.name, p.id, o.value
from options o
join profiles p on p.deprecated_uuid = o.profile;

insert into options_new(name, profile_id, value)
select o.name, 0, o.value
from options o
where o.profile='-';

update options_new
set name='profile_id'
  , value=(select p.id from profiles p where p.deprecated_uuid=value)
where name='profile_uuid';

drop table options;
alter table options_new rename to options;

update options
set name='profile_id'
  , value=(select p.id from profiles p where p.deprecated_uuid=options.value)
where name='profile_uuid';

-- account_values
create table account_values_new(
id integer not null primary key,
account_id integer not null references accounts(id) on delete cascade on update restrict,
currency text not null default '',
value real not null,
generation integer not null default 0);

insert into account_values_new(account_id, currency, value, generation)
select a.id, av.currency, av.value, av.generation
from account_values av
join profiles p on p.deprecated_uuid=av.profile
join accounts a on a.profile_id = p.id and a.name = av.account;

drop table account_values;
alter table account_values_new rename to account_values;

create index fk_account_value_acc on account_values(account_id);
create unique index un_account_values on account_values(account_id, currency);

-- transactions
create table transactions_new(
id integer not null primary key,
profile_id integer not null references profiles(id) on delete cascade on update restrict,
ledger_id integer not null,
description text not null,
year integer not null,
month integer not null,
day integer not null,
comment text,
data_hash text not null,
generation integer not null);

insert into transactions_new(profile_id, ledger_id, description, year, month, day, comment, data_hash, generation)
select p.id, t.id, t.description, t.year, t.month, t.day, t.comment, t.data_hash, t.generation
from transactions t
join profiles p on p.deprecated_uuid = t.profile;

-- transaction_accounts
create table transaction_accounts_new(
    id integer not null primary key,
    transaction_id integer not null references transactions_new(id) on delete cascade on update restrict,
    order_no integer not null,
    account_name text not null,
    currency text not null default '',
    amount real not null,
    comment text,
    generation integer not null default 0);

insert into transaction_accounts_new(transaction_id, order_no, account_name,
    currency, amount, comment, generation)
select t.id, ta.order_no, ta.account_name, ta.currency, ta.amount, ta.comment, ta.generation
from transaction_accounts ta
join profiles p on ta.profile=p.deprecated_uuid
join transactions_new t on ta.transaction_id = t.ledger_id and t.profile_id=p.id;

drop table transaction_accounts;
alter table transaction_accounts_new rename to transaction_accounts;

drop table transactions;
alter table transactions_new rename to transactions;

create index idx_transaction_description on transactions(description);
create unique index un_transactions_ledger_id on transactions(profile_id, ledger_id);
create index fk_transaction_profile on transactions(profile_id);

create unique index un_transaction_accounts on transaction_accounts(transaction_id, order_no);
create index fk_trans_acc_trans on transaction_accounts(transaction_id);