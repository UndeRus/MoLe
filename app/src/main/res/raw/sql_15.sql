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

BEGIN TRANSACTION;

delete from options where profile is null and name='last_scrape';
create table new_options(profile varchar not null, name varchar not null, value varchar);

insert into new_options(profile, name, value) select distinct '-', o.name, (select o2.value from options o2 where o2.name=o.name and o2.profile is null) from options o where o.profile is null;
insert into new_options(profile, name, value) select distinct o.profile, o.name, (select o2.value from options o2 where o2.name=o.name and o2.profile=o.profile) from options o where o.profile is not null;
drop table options;
create table options(profile varchar not null, name varchar not null, value varchar);
create unique index un_options on options(profile,name);
insert into options(profile,name,value) select profile,name,value from new_options;
drop table new_options;

COMMIT TRANSACTION;