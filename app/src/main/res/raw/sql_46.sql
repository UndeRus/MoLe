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

drop table pattern_accounts;
drop table patterns;

create table patterns(id integer not null primary key, name text not null, position integer not null, regular_expression text not null, transaction_description text, transaction_description_match_group integer, transaction_comment text, transaction_comment_match_group integer, date_year integer, date_year_match_group integer, date_month integer, date_month_match_group integer, date_day integer, date_day_match_group integer);
create unique index un_patterns_id on patterns(id);
create table pattern_accounts(id integer not null primary key, pattern_id integer not null, position integer not null, acc text, acc_match_group integer, currency integer, currency_match_group integer, amount decimal, amount_match_group integer, comment text, comment_match_group integer, constraint fk_pattern_account_pattern foreign key(pattern_id) references patterns(id), constraint fk_pattern_account_currency foreign key(currency) references currencies(id));
create unique index un_pattern_accounts on pattern_accounts(id);
