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
create table description_history_new(description varchar not null primary key, description_upper varchar, generation integer default 0);
insert into description_history_new(description, description_upper) select description, description_upper from description_history;
drop table description_history;
alter table description_history_new rename to description_history;
create unique index un_description_history on description_history(description_upper);
