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

drop index idx_accounts_name;
create table accounts_tmp(name varchar not null, name_upper varchar not null primary key, hidden boolean not null default 0, level integer not null default 0, parent_name varchar);
insert or replace into accounts_tmp(name, name_upper, hidden, level, parent_name) select name, name_upper, hidden, level, parent from accounts;
drop table accounts;
create table accounts(name varchar not null, name_upper varchar not null primary key, hidden boolean not null default 0, level integer not null default 0, parent_name varchar, keep boolean default 1);
insert into accounts(name, name_upper, hidden, level, parent_name) select name, name_upper, hidden, level, parent_name from accounts_tmp;
drop table accounts_tmp;

COMMIT TRANSACTION;