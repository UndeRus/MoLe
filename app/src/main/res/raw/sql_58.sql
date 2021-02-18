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

-- profiles ground for  Room

PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;

create table profiles(
 uuid text not null,
 name text not null,
 url text not null,
 use_authentication integer not null,
 auth_user text,
 auth_password text,
 order_no integer not null,
 permit_posting integer not null default 0,
 theme integer not null default -1,
 preferred_accounts_filter varchar,
 future_dates integer not null,
 api_version integer not null,
 show_commodity_by_default integer not null default 0,
 default_commodity text,
 show_comments_by_default integer not null default 1,
 detected_version_pre_1_19 integer not null,
 detected_version_major integer not null,
 detected_version_minor integer not null,
 primary key(uuid));

insert into profiles_new(
 uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 detected_version_pre_1_19, detected_version_major, detected_version_minor)
select uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 detected_version_pre_1_19, detected_version_major, detected_version_minor
from profiles;

COMMIT TRANSACTION;

PRAGMA foreign_keys = ON;