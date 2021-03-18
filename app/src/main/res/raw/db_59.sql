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

-- migrate from revision 58 to revision 59

CREATE TABLE profiles_new (
id INTEGER NOT NULL PRIMARY KEY,
uuid text,
name TEXT NOT NULL,
url TEXT NOT NULL,
use_authentication INTEGER NOT NULL,
auth_user TEXT,
auth_password TEXT,
order_no INTEGER NOT NULL,
permit_posting INTEGER NOT NULL,
theme INTEGER NOT NULL DEFAULT -1,
preferred_accounts_filter TEXT,
future_dates INTEGER NOT NULL,
api_version INTEGER NOT NULL,
show_commodity_by_default INTEGER NOT NULL,
default_commodity TEXT,
show_comments_by_default INTEGER NOT NULL DEFAULT 1,
detected_version_pre_1_19 INTEGER NOT NULL,
detected_version_major INTEGER NOT NULL,
detected_version_minor INTEGER NOT NULL);

insert into profiles_new(
       uuid, name, url, use_authentication, auth_user, auth_password,
       order_no, permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
       show_commodity_by_default, default_commodity, show_comments_by_default, detected_version_pre_1_19,
       detected_version_major, detected_version_minor)
select uuid, name, url, use_authentication, auth_user, auth_password,
       order_no, permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
       show_commodity_by_default, default_commodity, show_comments_by_default, detected_version_pre_1_19,
       detected_version_major, detected_version_minor
from profiles;

create table accounts_new(
id integer primary key not null,
profile_id integer not null references profiles_new(id) on delete cascade on update restrict,
level INTEGER NOT NULL,
name TEXT NOT NULL,
name_upper TEXT NOT NULL,
parent_name TEXT,
expanded INTEGER NOT NULL DEFAULT 1,
amounts_expanded INTEGER NOT NULL DEFAULT 0,
generation INTEGER NOT NULL DEFAULT 0);

insert into accounts_new(profile_id, level, name, name_upper, parent_name, expanded, amounts_expanded, generation)
select p.id, a.level, a.name, a.name_upper, a.parent_name, a.expanded, a.amounts_expanded, a.generation
from profiles_new p
join accounts a on a.profile=p.uuid;

drop table accounts;
alter table accounts_new rename to accounts;

drop table profiles;
alter table profiles_new rename to profiles;