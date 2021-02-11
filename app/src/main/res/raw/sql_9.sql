-- Copyright © 2018 Damyan Ivanov.
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

BEGIN TRANSACTION;

alter table transactions add keep boolean default 1 not null;
update transactions set keep = 1;
create table transactions_new(id integer, date varchar, description varchar, data_hash varchar, keep boolean);
insert into transactions_new(id, date, description, data_hash, keep) select cast(id as integer), date, description, data_hash, keep from transactions;
drop table transactions;
create table transactions(id integer primary key, date varchar, description varchar, data_hash varchar, keep boolean);
create unique index un_transactions_data_hash on transactions(data_hash);
insert into transactions(id, date, description, data_hash, keep) select id, date, description, data_hash, keep from transactions_new;
drop table transactions_new;

COMMIT TRANSACTION;