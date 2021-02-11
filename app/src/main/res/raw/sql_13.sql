-- Copyright © 2019 Damyan Ivanov.
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

delete from options where name='transaction_list_last_update';
delete from options where name='last_refresh';
alter table options add profile varchar;
drop index idx_options_name;
create unique index un_options on options(profile,name);
--
drop table account_values;
create table account_values(profile varchar not null, account varchar not null, currency varchar not null default '', keep boolean, value decimal not null );
create unique index un_account_values on account_values(profile,account,currency);
--
drop table accounts;
create table accounts(profile varchar not null, name varchar not null, name_upper varchar not null, hidden boolean not null default 0, keep boolean not null default 0, level integer not null, parent_name varchar);
create unique index un_accounts on accounts(profile, name);
--
drop table transaction_accounts;
drop table transactions;
--
create table transactions(id integer not null, data_hash varchar not null, date varchar not null, description varchar not null, keep boolean not null default 0);
create unique index un_transactions_id on transactions(id);
create unique index un_transactions_data_hash on transactions(data_hash);
--
create table transaction_accounts(profile varchar not null, transaction_id integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,account_name), constraint fk_transaction_accounts_trn foreign key(transaction_id) references transactions(id));
