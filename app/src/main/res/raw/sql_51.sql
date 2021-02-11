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

create table patterns_new(id INTEGER not null primary key, name TEXT not null, regular_expression TEXT not null, transaction_description TEXT, transaction_description_match_group INTEGER, transaction_comment TEXT, transaction_comment_match_group INTEGER, date_year INTEGER, date_year_match_group INTEGER, date_month INTEGER, date_month_match_group INTEGER, date_day INTEGER, date_day_match_group INTEGER);

insert into patterns_new(id, name, regular_expression, transaction_description, transaction_description_match_group, transaction_comment, transaction_comment_match_group, date_year, date_year_match_group, date_month, date_month_match_group, date_day, date_day_match_group) select id, name, regular_expression, transaction_description, transaction_description_match_group, transaction_comment, transaction_comment_match_group, date_year, date_year_match_group, date_month, date_month_match_group, date_day, date_day_match_group from patterns;

drop table patterns;

alter table patterns_new rename to patterns;
create unique index un_patterns_id on patterns(id);
