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

-- profiles ground for  Room

PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;

create table profiles(
 uuid text not null,
 name text not null,
 url text not null,
 use_authentication integer not null,
 auth_user text,
 auth_password text,
 order_no integer not null,
 permit_posting integer not null default 0,
 theme integer not null default -1,
 preferred_accounts_filter varchar,
 future_dates integer not null,
 api_version integer not null,
 show_commodity_by_default integer not null default 0,
 default_commodity text,
 show_comments_by_default integer not null default 1,
 detected_version_pre_1_19 integer not null,
 detected_version_major integer not null,
 detected_version_minor integer not null,
 primary key(uuid));

insert into profiles_new(
 uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 detected_version_pre_1_19, detected_version_major, detected_version_minor)
select uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 detected_version_pre_1_19, detected_version_major, detected_version_minor
from profiles;

drop table profiles;

alter table profiles_new
rename to profiles;

create table options_new(profile varchar not null, name varchar not null, value varchar, primary key(profile, name));

insert into options_new(profile, name, value)
select profile, name, value from options;

drop table options;

alter table options_new
rename to options;

create table account_values_new(
 profile varchar not null,
 account varchar not null,
 currency varchar not null default '',
 value real not null,
 generation integer not null default 0,
 primary key(profile, account, currency));

insert into account_values_new(
 profile, account, currency, value, generation)
select profile, account, currency, value, generation
from account_values;

drop table account_values;
alter table account_values_new rename to account_values;

create table description_history_new(
 description varchar collate NOCASE not null primary key,
 description_upper varchar not null,
 generation integer not null default 0,
 primary key(description));

insert into description_history_new(description, description_upper, generation)
select description, description_upper, generation from description_history;

drop table description_history;
alter table description_history_new rename to description_history;

-- transactions

create table transactions_new(
 profile varchar not null,
 id integer not null,
 data_hash varchar not null,
 year integer not null,
 month integer not null,
 day integer not null,
 description varchar collate NOCASE not null,
 comment varchar,
 generation integer not null default 0,
 primary key(profile,id));

insert into transactions_new(profile, id, data_hash, year, month, day, description,
 comment, generation)
select profile, id, data_hash, year, month, day, description,
       comment, generation
from transactions;

drop table transactions;
alter table transactions_new rename to transactions;

create unique index un_transactions_data_hash on transactions(profile,data_hash);
create index idx_transaction_description on transactions(description);


COMMIT TRANSACTION;

PRAGMA foreign_keys = ON;