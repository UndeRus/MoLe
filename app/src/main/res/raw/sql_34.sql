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
alter table transactions add year integer not null default 0;
alter table transactions add month integer not null default 0;
alter table transactions add day integer not null default 0;
alter table transactions add tmp_md varchar;
update transactions set year= cast(substr(date,  1,instr(date,  '/')-1) as integer);
update transactions set tmp_md=    substr(date,    instr(date,  '/')+1);
update transactions set month=cast(substr(tmp_md,1,instr(tmp_md,'/')-1) as integer);
update transactions set day=  cast(substr(tmp_md,  instr(tmp_md,'/')+1) as integer);
-- alter table transactions drop date
create table transactions_2(profile varchar not null, id integer not null, data_hash varchar not null, year integer not null, month integer not null, day integer not null, description varchar not null, comment varchar, keep boolean not null default 0);
insert into transactions_2(profile, id, data_hash, year, month, day, description, comment, keep) select profile, id, data_hash, year, month, day, description, comment, keep from transactions;
drop table transactions;
alter table transactions_2 rename to transactions;
