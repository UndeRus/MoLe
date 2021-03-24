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

-- migrate from revision 41 to revision 58

-- profiles
create table profiles_new(
 uuid text not null,
 name text not null,
 url text not null,
 use_authentication integer not null,
 auth_user text,
 auth_password text,
 order_no integer not null,
 permit_posting integer not null default 0,
 theme integer not null default -1,
 preferred_accounts_filter varchar,
 future_dates integer not null,
 api_version integer not null,
 show_commodity_by_default integer not null default 0,
 default_commodity text,
 show_comments_by_default integer not null default 1,
 detected_version_pre_1_19 integer not null,
 detected_version_major integer not null,
 detected_version_minor integer not null,
 primary key(uuid));

insert into profiles_new(
 uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 detected_version_pre_1_19, detected_version_major, detected_version_minor)
select uuid, name, url, use_authentication, auth_user, auth_password, order_no,
 permit_posting, theme, preferred_accounts_filter, future_dates, api_version,
 show_commodity_by_default, default_commodity, show_comments_by_default,
 coalesce(detected_version_pre_1_19,0), coalesce(detected_version_major,0),
 coalesce(detected_version_minor,0)
from profiles;

-- options
create table options_new(profile varchar not null, name varchar not null, value varchar, primary key(profile, name));

insert into options_new(profile, name, value)
select profile, name, value from options;

-- accounts
create table accounts_new(
    profile varchar not null,
    name varchar not null,
    name_upper varchar not null,
    level integer not null,
    parent_name varchar,
    expanded integer not null default 1,
    amounts_expanded integer not null default 0,
    generation integer not null default 0,
    primary key(profile, name));

insert into accounts_new(profile, name, name_upper, level, parent_name,
    expanded, amounts_expanded, generation)
select profile, name, name_upper, level, parent_name, expanded,
    amounts_expanded, generation from accounts;

-- account_values
create table account_values_new(
    profile varchar not null,
    account varchar not null,
    currency varchar not null default '',
    value real not null,
    generation integer not null default 0,
    primary key(profile, account, currency));

insert into account_values_new(
    profile, account, currency, value, generation)
select profile, account, currency, value, generation
from account_values;

-- description_history
create table description_history_new(
    description varchar collate NOCASE not null,
    description_upper varchar not null,
    generation integer not null default 0,
    primary key(description));

insert into description_history_new(description, description_upper, generation)
select description, description_upper, generation from description_history;

-- transactions
create table transactions_new(
    profile varchar not null,
    id integer not null,
    data_hash varchar not null,
    year integer not null,
    month integer not null,
    day integer not null,
    description varchar collate NOCASE not null,
    comment varchar,
    generation integer not null default 0,
    primary key(profile,id));

insert into transactions_new(profile, id, data_hash, year, month, day, description,
    comment, generation)
select profile, id, data_hash, year, month, day, description,
       comment, generation
from transactions;

-- transaction_accounts
create table transaction_accounts_new(
    profile varchar not null,
    transaction_id integer not null,
    order_no integer not null,
    account_name varchar not null,
    currency varchar not null default '',
    amount real not null,
    comment varchar,
    generation integer not null default 0,
    primary key(profile, transaction_id, order_no),
    foreign key (profile,account_name) references accounts(profile,name)
      on delete cascade on update restrict,
    foreign key(profile, transaction_id) references transactions(profile,id)
      on delete cascade on update restrict);

insert into transaction_accounts_new(profile, transaction_id, order_no, account_name,
    currency, amount, comment, generation)
select profile, transaction_id, order_no, account_name,
       currency, amount, comment, generation
from transaction_accounts;

--currencies
create table currencies_new(id integer not null primary key, name varchar not null,
    position varchar not null, has_gap integer not null);

insert into currencies_new(id, name, position, has_gap)
select id, name, position, has_gap
from currencies;


-- drop originals
drop table transaction_accounts;
drop table transactions;
drop table account_values;
drop table accounts;
drop table description_history;
drop table profiles;
drop table options;
drop table currencies;

-- rename new
alter table options_new              rename to options;
alter table profiles_new             rename to profiles;
alter table accounts_new             rename to accounts;
alter table account_values_new       rename to account_values;
alter table description_history_new  rename to description_history;
alter table transactions_new         rename to transactions;
alter table transaction_accounts_new rename to transaction_accounts;
alter table currencies_new           rename to currencies;

-- indices
create        index fk_tran_acc_prof_acc        on transaction_accounts(profile, account_name);
create unique index un_transactions_data_hash   on transactions(profile,data_hash);
create        index idx_transaction_description on transactions(description);


-- new tables
CREATE TABLE templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
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
CREATE TABLE template_accounts(
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    template_id INTEGER NOT NULL,
    acc TEXT,
    position INTEGER NOT NULL,
    acc_match_group INTEGER,
    currency INTEGER,
    currency_match_group INTEGER,
    amount REAL,
    amount_match_group INTEGER,
    comment TEXT,
    comment_match_group INTEGER,
    negate_amount INTEGER,
    FOREIGN KEY(template_id) REFERENCES templates(id) ON UPDATE RESTRICT ON DELETE CASCADE,
    FOREIGN KEY(currency) REFERENCES currencies(id) ON UPDATE RESTRICT ON DELETE RESTRICT);
create index fk_template_accounts_template on template_accounts(template_id);
create index fk_template_accounts_currency on template_accounts(currency);
