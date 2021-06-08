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

-- migrate from revision 60 to revision 61

-- pragmas need to be outside of transaction control
-- foreign_keys is needed so that foreign key constraints are redirected

commit transaction;
pragma foreign_keys = on;

begin transaction;

alter table transactions
add description_uc text not null default '';

update transactions
set description_uc=upper(description);

delete from options where name='last_scrape';