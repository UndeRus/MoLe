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

-- migrate from revision 62 to revision 63

-- pragmas need to be outside of transaction control
-- foreign_keys is needed so that foreign key constraints are redirected

commit transaction;
pragma foreign_keys = off;

begin transaction;

CREATE TABLE new_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    uuid TEXT NOT NULL,
    regular_expression TEXT NOT NULL,
    test_text TEXT,
    transaction_description TEXT,
    transaction_description_match_group INTEGER,
    transaction_comment TEXT,
    transaction_comment_match_group INTEGER,
    date_year INTEGER,
    date_year_match_group INTEGER,
    date_month INTEGER,
    date_month_match_group INTEGER,
    date_day INTEGER,
    date_day_match_group INTEGER,
    is_fallback INTEGER NOT NULL DEFAULT 0);

insert into new_templates(id, name, uuid, regular_expression, test_text,
    transaction_description, transaction_description_match_group,
    transaction_comment, transaction_comment_match_group,
    date_year, date_year_match_group,
    date_month, date_month_match_group,
    date_day, date_day_match_group,
    is_fallback)
select id, name, random(), regular_expression, test_text,
       transaction_description, transaction_description_match_group,
       transaction_comment, transaction_comment_match_group,
       date_year, date_year_match_group,
       date_month, date_month_match_group,
       date_day, date_day_match_group,
       is_fallback
from templates;

drop table templates;
alter table new_templates rename to templates;

create unique index templates_uuid_idx on templates(uuid);