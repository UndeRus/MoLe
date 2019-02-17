alter table profiles add theme integer default -1;
update profiles set theme = -1;