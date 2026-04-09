alter table platform_users
  add column credential_version int not null default 0 after enabled;
