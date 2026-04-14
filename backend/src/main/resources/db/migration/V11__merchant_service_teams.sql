create table service_teams (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  name varchar(120) not null,
  code varchar(80) not null,
  status varchar(32) not null,
  created_at datetime not null,
  primary key (id),
  unique key uk_service_teams_merchant_code (merchant_id, code),
  key idx_service_teams_merchant_id (merchant_id),
  constraint fk_service_teams_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table team_members (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  team_id bigint not null,
  user_id bigint not null,
  role varchar(64) not null,
  status varchar(32) not null,
  primary key (id),
  unique key uk_team_members_team_user (team_id, user_id),
  key idx_team_members_merchant_team (merchant_id, team_id),
  key idx_team_members_user_id (user_id),
  constraint fk_team_members_merchant
    foreign key (merchant_id) references merchants (id),
  constraint fk_team_members_team
    foreign key (team_id) references service_teams (id),
  constraint fk_team_members_user
    foreign key (user_id) references platform_users (id)
) engine=InnoDB default charset=utf8mb4;
