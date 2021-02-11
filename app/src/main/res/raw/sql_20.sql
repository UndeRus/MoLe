-- Copyright © 2019 Damyan Ivanov.
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

delete from accounts where not exists (select 1 from profiles where uuid = profile);
delete from account_values where not exists (select 1 from profiles where uuid = profile);
delete from transactions where not exists (select 1 from profiles where uuid = profile);
delete from transaction_accounts where not exists (select 1 from profiles where uuid = profile);
