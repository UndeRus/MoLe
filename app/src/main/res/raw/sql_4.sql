alter table accounts add hidden boolean default 0;
update accounts set hidden = 0;