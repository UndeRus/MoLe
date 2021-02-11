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

-- copied from the Room migration

CREATE TABLE template_accounts_new(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, template_id INTEGER NOT NULL, acc TEXT, position INTEGER NOT NULL, acc_match_group INTEGER, currency INTEGER, currency_match_group INTEGER, amount REAL, amount_match_group INTEGER, comment TEXT, comment_match_group INTEGER, negate_amount INTEGER, FOREIGN KEY(template_id) REFERENCES templates(id) ON UPDATE RESTRICT ON DELETE CASCADE, FOREIGN KEY(currency) REFERENCES currencies(id) ON UPDATE RESTRICT ON DELETE RESTRICT);
insert into template_accounts_new(id, template_id, acc, position, acc_match_group, currency, currency_match_group, amount, amount_match_group, amount, amount_match_group, comment, comment_match_group, negate_amount) select id, template_id, acc, position, acc_match_group, currency, currency_match_group, amount, amount_match_group, amount, amount_match_group, comment, comment_match_group, negate_amount from template_accounts;
drop table template_accounts;
alter table template_accounts_new rename to template_accounts;

create index fk_template_accounts_template on template_accounts(template_id);
create index fk_template_accounts_currency on template_accounts(currency);
