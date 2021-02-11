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

drop table transaction_accounts;
drop table transactions;
--
create table transactions(profile varchar not null, id integer not null, data_hash varchar not null, date varchar not null, description varchar not null, keep boolean not null default 0);
create unique index un_transactions_id on transactions(profile,id);
create unique index un_transactions_data_hash on transactions(profile,data_hash);
--
create table transaction_accounts(profile varchar not null, transaction_id integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,account_name), constraint fk_transaction_accounts_trn foreign key(profile, transaction_id) references transactions(profile,id));
