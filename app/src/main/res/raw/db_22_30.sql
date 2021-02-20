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

-- migrate from revision 22 to revision 30

-- 23, 24, 27, 28
alter table profiles
add future_dates integer,
add api_version integer,
add show_commodity_by_default boolean default 0,
add default_commodity varchar;

-- 25
create table currencies(id integer not null primary key, name varchar not null, position varchar not null, has_gap boolean not null);

-- 26
alter table transaction_accounts add comment varchar;

-- 29
create index idx_transaction_description on transactions(description);

-- 30
delete from options
where profile <> '-'
  and not exists (select 1 from profiles p where p.uuid=options.profile);