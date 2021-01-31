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
create table profiles(uuid varchar not null primary key, name not null, url not null, use_authentication boolean not null, auth_user varchar, auth_password varchar, order_no integer, permit_posting boolean default 0, theme integer default -1, preferred_accounts_filter varchar, future_dates integer, api_version integer, show_commodity_by_default boolean default 0, default_commodity varchar, show_comments_by_default boolean default 1, detected_version_pre_1_19 boolean, detected_version_major integer, detected_version_minor integer);
create table accounts(profile varchar not null, name varchar not null, name_upper varchar not null, level integer not null, parent_name varchar, expanded default 1, amounts_expanded boolean default 0, generation integer default 0);
create unique index un_accounts on accounts(profile, name);
create table options(profile varchar not null, name varchar not null, value varchar);
create unique index un_options on options(profile,name);
create table account_values(profile varchar not null, account varchar not null, currency varchar not null default '', value decimal not null, generation integer default 0 );
create unique index un_account_values on account_values(profile,account,currency);
create table description_history(description varchar not null primary key, description_upper varchar, generation integer default 0);
create unique index un_description_history on description_history(description_upper);
create table transactions(profile varchar not null, id integer not null, data_hash varchar not null, year integer not null, month integer not null, day integer not null, description varchar not null, comment varchar, generation integer default 0);
create unique index un_transactions_id on transactions(profile,id);
create unique index un_transactions_data_hash on transactions(profile,data_hash);
create index idx_transaction_description on transactions(description);
create table transaction_accounts(profile varchar not null, transaction_id integer not null, order_no integer not null, account_name varchar not null, currency varchar not null default '', amount decimal not null, comment varchar, generation integer default 0, constraint fk_transaction_accounts_acc foreign key(profile,account_name) references accounts(profile,name), constraint fk_transaction_accounts_trn foreign key(profile, transaction_id) references transactions(profile,id));
create unique index un_transaction_accounts_order on transaction_accounts(profile, transaction_id, order_no);
create table currencies(id integer not null primary key, name varchar not null, position varchar not null, has_gap integer not null);

create table patterns(id INTEGER not null primary key, name TEXT not null, regular_expression TEXT not null, test_text TEXT, transaction_description TEXT, transaction_description_match_group INTEGER, transaction_comment TEXT, transaction_comment_match_group INTEGER, date_year INTEGER, date_year_match_group INTEGER, date_month INTEGER, date_month_match_group INTEGER, date_day INTEGER, date_day_match_group INTEGER);
create unique index un_patterns_id on patterns(id);
create table pattern_accounts(id INTEGER not null primary key, pattern_id INTEGER not null, position INTEGER not null, acc TEXT, acc_match_group INTEGER, currency INTEGER, currency_match_group INTEGER, amount REAL, amount_match_group INTEGER, comment TEXT, comment_match_group INTEGER, constraint fk_pattern_account_pattern foreign key(pattern_id) references patterns(id), constraint fk_pattern_account_currency foreign key(currency) references currencies(id));
create unique index un_pattern_accounts on pattern_accounts(id);
create index fk_pattern_accounts_pattern on pattern_accounts(pattern_id);
create index fk_pattern_accounts_currency on pattern_accounts(currency);