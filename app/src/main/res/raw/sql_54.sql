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

-- copied from the Room migration

CREATE TABLE templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, regular_expression TEXT NOT NULL, test_text TEXT, transaction_description TEXT, transaction_description_match_group INTEGER, transaction_comment TEXT, transaction_comment_match_group INTEGER, date_year INTEGER, date_year_match_group INTEGER, date_month INTEGER, date_month_match_group INTEGER, date_day INTEGER, date_day_match_group INTEGER);
CREATE TABLE template_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, template_id INTEGER NOT NULL, acc TEXT, position INTEGER NOT NULL, acc_match_group INTEGER, currency INTEGER, currency_match_group INTEGER, amount REAL, amount_match_group INTEGER, comment TEXT, comment_match_group INTEGER, negate_amount INTEGER, FOREIGN KEY(template_id) REFERENCES templates(id) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(currency) REFERENCES currencies(id) ON UPDATE NO ACTION ON DELETE NO ACTION);
insert into templates(id, name, regular_expression, test_text, transaction_description, transaction_description_match_group, transaction_comment, transaction_comment_match_group, date_year, date_year_match_group, date_month, date_month_match_group, date_day, date_day_match_group) select id, name, regular_expression, test_text, transaction_description, transaction_description_match_group, transaction_comment, transaction_comment_match_group, date_year, date_year_match_group, date_month, date_month_match_group, date_day, date_day_match_group from patterns;
insert into template_accounts(id, template_id, acc, position, acc_match_group, currency, currency_match_group, amount, amount_match_group, amount, amount_match_group, comment, comment_match_group, negate_amount) select id, pattern_id, acc, position, acc_match_group, currency, currency_match_group, amount, amount_match_group, amount, amount_match_group, comment, comment_match_group, negate_amount from pattern_accounts;
create index fk_template_accounts_template on template_accounts(template_id);
create index fk_template_accounts_currency on template_accounts(currency);
drop table pattern_accounts;
drop table patterns;