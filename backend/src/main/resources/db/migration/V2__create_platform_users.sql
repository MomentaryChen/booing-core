create table platform_users (
  id bigint not null auto_increment,
  username varchar(120) not null,
  password_hash varchar(255) not null,
  platform_role varchar(32) not null,
  merchant_id bigint,
  enabled bit not null default b'1',
  created_at datetime(6) not null default current_timestamp(6),
  updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
  last_login_at datetime(6),
  primary key (id),
  unique key uk_platform_users_username (username),
  key idx_platform_users_merchant_id (merchant_id),
  constraint fk_platform_users_merchant foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

