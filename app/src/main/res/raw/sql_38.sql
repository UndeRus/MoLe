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
CREATE TABLE transaction_accounts_new(profile varchar not null, transaction_id integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, comment varchar, generation integer default 0, order_no integer not null default 0, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,name), constraint fk_transaction_accounts_trn foreign key(profile, transaction_id) references transactions(profile,id));
insert into transaction_accounts_new(profile, transaction_id, account_name, currency, amount, comment, generation, order_no) select profile, transaction_id, account_name, currency, amount, comment, generation, order_no from transaction_accounts;
drop table transaction_accounts;
alter table transaction_accounts_new rename to transaction_accounts;
create unique index un_transaction_accounts_order on transaction_accounts(profile, transaction_id, order_no);