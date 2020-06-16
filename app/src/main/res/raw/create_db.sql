-- Copyright © 2020 Damyan Ivanov.
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
create table accounts(profile varchar not null, name varchar not null, name_upper varchar not null, hidden boolean not null default 0, keep boolean not null default 0, level integer not null, parent_name varchar, expanded default 1, amounts_expanded boolean default 0);
create unique index un_accounts on accounts(profile, name);
create table options(profile varchar not null, name varchar not null, value varchar);
create unique index un_options on options(profile,name);
create table account_values(profile varchar not null, account varchar not null, currency varchar not null default '', keep boolean, value decimal not null );
create unique index un_account_values on account_values(profile,account,currency);
create table description_history(description varchar not null primary key, keep boolean, description_upper varchar);
create table profiles(uuid varchar not null primary key, name not null, url not null, use_authentication boolean not null, auth_user varchar, auth_password varchar, order_no integer, permit_posting boolean default 0, theme integer default -1, preferred_accounts_filter varchar, future_dates integer, api_version integer, show_commodity_by_default boolean default 0, default_commodity varchar, show_comments_by_default boolean default 1);
create table transactions(profile varchar not null, id integer not null, data_hash varchar not null, year integer not null, month integer not null, day integer not null, description varchar not null, comment varchar, keep boolean not null default 0);
create unique index un_transactions_id on transactions(profile,id);
create unique index un_transactions_data_hash on transactions(profile,data_hash);
create index idx_transaction_description on transactions(description);
create table transaction_accounts(profile varchar not null, transaction_id integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, comment varchar, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,account_name), constraint fk_transaction_accounts_trn foreign key(profile, transaction_id) references transactions(profile,id));
create table currencies(id integer not null primary key, name varchar not null, position varchar not null, has_gap boolean not null);
-- updated to revision 34