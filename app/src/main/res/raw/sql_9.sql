alter table transactions add keep boolean default 1 not null;
update transactions set keep = 1;
create table transactions_new(id integer, date varchar, description varchar, data_hash varchar, keep boolean);
insert into transactions_new(id, date, description, data_hash, keep) select cast(id as integer), date, description, data_hash, keep from transactions;
drop table transactions;
create table transactions(id integer primary key, date varchar, description varchar, data_hash varchar, keep boolean);
create unique index un_transactions_data_hash on transactions(data_hash);
insert into transactions(id, date, description, data_hash, keep) select id, date, description, data_hash, keep from transactions_new;
drop table transactions_new;