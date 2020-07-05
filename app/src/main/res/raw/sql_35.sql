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
create table accounts_new(profile varchar not null, name varchar not null, name_upper varchar not null, keep boolean not null default 0, level integer not null, parent_name varchar, expanded default 1, amounts_expanded boolean default 0);
insert into accounts_new(profile, name, name_upper, keep, level, parent_name, expanded, amounts_expanded) select profile, name, name_upper, keep, level, parent_name, expanded, amounts_expanded from accounts;
drop table accounts;
alter table accounts_new rename to accounts;
