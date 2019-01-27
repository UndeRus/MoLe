alter table profiles add permit_posting boolean default 0;
update profiles set permit_posting = 1;