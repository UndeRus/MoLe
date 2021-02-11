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

pragma foreign_keys=off;

BEGIN TRANSACTION;

create table currencies_new(id integer not null primary key, name varchar not null, position varchar not null, has_gap integer not null);
insert into currencies_new(id, name, position, has_gap) select id, name, position, has_gap from currencies;

drop table currencies;

alter table currencies_new rename to currencies;

COMMIT TRANSACTION;