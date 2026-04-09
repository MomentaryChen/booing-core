alter table platform_users
  add column failed_login_count int not null default 0 after credential_version,
  add column failed_login_window_started_at datetime(6) null after failed_login_count,
  add column locked_until datetime(6) null after failed_login_window_started_at;
