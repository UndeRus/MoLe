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

-- migrate from revision 34 to revision 40

-- 35
create table accounts_new(
    profile varchar not null,
    name varchar not null,
    name_upper varchar not null,
    keep boolean not null default 0,
    level integer not null,
    parent_name varchar,
    expanded default 1,
    amounts_expanded boolean default 0,
    generation integer default 0);

insert into accounts_new(
    profile, name, name_upper, keep, level, parent_name, expanded, amounts_expanded)
select profile, name, name_upper, keep, level, parent_name, expanded, amounts_expanded
from accounts;

drop table accounts;

alter table accounts_new rename to accounts;

-- 36
-- merged in 35 --alter table accounts add generation integer default 0;

alter table account_values add generation integer default 0;

alter table transactions add generation integer default 0;

alter table transaction_accounts
add generation integer default 0,
add order_no integer not null default 0;

-- 37
update transaction_accounts set order_no = rowid;

-- 40
delete from transaction_accounts where not exists (select 1 from accounts a where a.profile=transaction_accounts.profile and a.name=transaction_accounts.account_name);
delete from transaction_accounts where not exists (select 1 from transactions t where t.profile=transaction_accounts.profile and t.id=transaction_accounts.transaction_id);

-- 38
CREATE TABLE transaction_accounts_new(profile varchar not null, transaction_id integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, comment varchar, generation integer default 0, order_no integer not null default 0, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,name), constraint fk_transaction_accounts_trn foreign key(profile, transaction_id) references transactions(profile,id));
insert into transaction_accounts_new(profile, transaction_id, account_name, currency, amount, comment, generation, order_no) select profile, transaction_id, account_name, currency, amount, comment, generation, order_no from transaction_accounts;
drop table transaction_accounts;
alter table transaction_accounts_new rename to transaction_accounts;
create unique index un_transaction_accounts_order on transaction_accounts(profile, transaction_id, order_no);

-- 39
create table description_history_new(description varchar not null primary key, description_upper varchar, generation integer default 0);
insert into description_history_new(description, description_upper) select description, description_upper from description_history;
drop table description_history;
alter table description_history_new rename to description_history;
create unique index un_description_history on description_history(description_upper);

