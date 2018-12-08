alter table description_history add description_upper varchar;
update description_history set description_upper = upper(description);
alter table accounts add name_upper varchar;
update accounts set name_upper = upper(name);