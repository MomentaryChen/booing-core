-- booking-core schema baseline (MySQL)
-- Flyway migration: V1

create table merchants (
  id bigint not null auto_increment,
  name varchar(120) not null,
  slug varchar(120) not null,
  active bit not null,
  service_limit int not null,
  primary key (id),
  unique key uk_merchants_name (name),
  unique key uk_merchants_slug (slug)
) engine=InnoDB default charset=utf8mb4;

create table merchant_profiles (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  description varchar(500),
  logo_url varchar(255),
  primary key (id),
  unique key uk_merchant_profiles_merchant_id (merchant_id),
  constraint fk_merchant_profiles_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table customization_configs (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  theme_color varchar(40),
  theme_preset varchar(40) not null,
  hero_title varchar(80),
  booking_flow_text varchar(300),
  invite_code varchar(80),
  terms_text varchar(2000),
  announcement_text varchar(2000),
  faq_json tinytext not null,
  buffer_minutes int not null,
  homepage_sections_json tinytext not null,
  category_order_json tinytext not null,
  primary key (id),
  unique key uk_customization_configs_merchant_id (merchant_id),
  constraint fk_customization_configs_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table dynamic_field_configs (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  label varchar(120) not null,
  type varchar(40) not null,
  required_field bit not null,
  options_json tinytext not null,
  primary key (id),
  key idx_dynamic_field_configs_merchant_id (merchant_id),
  constraint fk_dynamic_field_configs_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table resource_items (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  name varchar(120) not null,
  type varchar(40) not null,
  category varchar(80) not null,
  capacity int not null,
  service_items_json tinytext not null,
  price decimal(10,2) not null,
  active bit not null,
  primary key (id),
  key idx_resource_items_merchant_id (merchant_id),
  constraint fk_resource_items_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table service_items (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  name varchar(120) not null,
  duration_minutes int not null,
  price decimal(10,2) not null,
  category varchar(80) not null,
  primary key (id),
  key idx_service_items_merchant_id (merchant_id),
  constraint fk_service_items_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table business_hours (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  day_of_week varchar(16) not null,
  start_time time not null,
  end_time time not null,
  primary key (id),
  key idx_business_hours_merchant_id (merchant_id),
  constraint fk_business_hours_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table availability_exceptions (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  type varchar(24) not null,
  start_at datetime(6) not null,
  end_at datetime(6) not null,
  reason varchar(200),
  primary key (id),
  key idx_availability_exceptions_merchant_id (merchant_id),
  constraint fk_availability_exceptions_merchant
    foreign key (merchant_id) references merchants (id)
) engine=InnoDB default charset=utf8mb4;

create table bookings (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  service_item_id bigint not null,
  start_at datetime(6) not null,
  end_at datetime(6) not null,
  customer_name varchar(120) not null,
  customer_contact varchar(120) not null,
  status varchar(16) not null,
  primary key (id),
  key idx_bookings_merchant_id (merchant_id),
  key idx_bookings_service_item_id (service_item_id),
  constraint fk_bookings_merchant
    foreign key (merchant_id) references merchants (id),
  constraint fk_bookings_service_item
    foreign key (service_item_id) references service_items (id)
) engine=InnoDB default charset=utf8mb4;

create table platform_pages (
  id bigint not null auto_increment,
  route_key varchar(80) not null,
  frontend_path varchar(255) not null,
  label_key varchar(80) not null,
  sort_order int not null,
  active bit not null,
  primary key (id),
  unique key uk_platform_pages_route_key (route_key)
) engine=InnoDB default charset=utf8mb4;

create table role_page_grants (
  id bigint not null auto_increment,
  platform_role varchar(32) not null,
  page_id bigint not null,
  primary key (id),
  unique key uk_role_page_grants_role_page (platform_role, page_id),
  key idx_role_page_grants_page_id (page_id),
  constraint fk_role_page_grants_page
    foreign key (page_id) references platform_pages (id)
) engine=InnoDB default charset=utf8mb4;

create table system_settings (
  id bigint not null auto_increment,
  email_template varchar(2000),
  sms_template varchar(2000),
  maintenance_announcement varchar(2000),
  primary key (id)
) engine=InnoDB default charset=utf8mb4;

create table domain_templates (
  id bigint not null auto_increment,
  domain_name varchar(80) not null,
  fields_json tinytext not null,
  primary key (id),
  unique key uk_domain_templates_domain_name (domain_name)
) engine=InnoDB default charset=utf8mb4;

create table audit_logs (
  id bigint not null auto_increment,
  actor varchar(80) not null,
  action varchar(80) not null,
  target_type varchar(80) not null,
  target_id bigint not null,
  detail tinytext not null,
  created_at datetime(6) not null,
  primary key (id)
) engine=InnoDB default charset=utf8mb4;

