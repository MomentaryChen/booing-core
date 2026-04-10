alter table merchants
  add column visibility varchar(32) not null default 'PUBLIC';

create table merchant_invitations (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  invitee_user_id bigint not null,
  invite_code varchar(80) not null,
  status varchar(32) not null,
  expires_at datetime,
  created_by varchar(120) not null,
  created_at datetime not null,
  updated_at datetime not null,
  primary key (id),
  unique key uk_merchant_invitations_invite_code (invite_code),
  key idx_merchant_invitations_merchant_id (merchant_id),
  key idx_merchant_invitations_invitee_status (invitee_user_id, status),
  constraint fk_merchant_invitations_merchant
    foreign key (merchant_id) references merchants (id),
  constraint fk_merchant_invitations_invitee_user
    foreign key (invitee_user_id) references platform_users (id)
) engine=InnoDB default charset=utf8mb4;

create table merchant_memberships (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  platform_user_id bigint not null,
  membership_status varchar(32) not null,
  joined_at datetime not null,
  updated_at datetime not null,
  primary key (id),
  unique key uk_merchant_memberships_merchant_user (merchant_id, platform_user_id),
  key idx_merchant_memberships_user_status (platform_user_id, membership_status),
  constraint fk_merchant_memberships_merchant
    foreign key (merchant_id) references merchants (id),
  constraint fk_merchant_memberships_platform_user
    foreign key (platform_user_id) references platform_users (id)
) engine=InnoDB default charset=utf8mb4;
