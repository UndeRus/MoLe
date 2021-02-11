-- Copyright © 2018 Damyan Ivanov.
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

create table if not exists accounts(name varchar);
create index if not exists idx_accounts_name on accounts(name);
create table if not exists options(name varchar, value varchar);
create unique index if not exists idx_options_name on options(name);
create table if not exists account_values(account varchar not null, currency varchar not null, value decimal(18,2) not null);
create index if not exists idx_account_values_account on account_values(account);
create unique index if not exists un_account_values on account_values(account,currency);

COMMIT TRANSACTION;