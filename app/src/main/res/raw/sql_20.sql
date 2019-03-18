delete from accounts where not exists (select 1 from profiles where uuid = profile);
delete from account_values where not exists (select 1 from profiles where uuid = profile);
delete from transactions where not exists (select 1 from profiles where uuid = profile);
delete from transaction_accounts where not exists (select 1 from profiles where uuid = profile);