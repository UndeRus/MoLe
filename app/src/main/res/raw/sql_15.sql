delete from options where profile is null and name='last_scrape';
create table new_options(profile varchar not null, name varchar not null, value varchar);

insert into new_options(profile, name, value) select distinct '-', o.name, (select o2.value from options o2 where o2.name=o.name and o2.profile is null) from options o where o.profile is null;
insert into new_options(profile, name, value) select distinct o.profile, o.name, (select o2.value from options o2 where o2.name=o.name and o2.profile=o.profile) from options o where o.profile is not null;
drop table options;
create table options(profile varchar not null, name varchar not null, value varchar);
create unique index un_options on options(profile,name);
insert into options(profile,name,value) select profile,name,value from new_options;
drop table new_options;