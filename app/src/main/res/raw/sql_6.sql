drop index idx_accounts_name;
create table accounts_tmp(name varchar not null, name_upper varchar not null primary key, hidden boolean not null default 0, level integer not null default 0, parent_name varchar);
insert or replace into accounts_tmp(name, name_upper, hidden, level, parent_name) select name, name_upper, hidden, level, parent from accounts;
drop table accounts;
create table accounts(name varchar not null, name_upper varchar not null primary key, hidden boolean not null default 0, level integer not null default 0, parent_name varchar, keep boolean default 1);
insert into accounts(name, name_upper, hidden, level, parent_name) select name, name_upper, hidden, level, parent_name from accounts_tmp;
drop table accounts_tmp;
