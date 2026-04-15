create table resource_staff_assignments (
  id bigint not null auto_increment,
  merchant_id bigint not null,
  booking_id bigint not null,
  resource_id bigint not null,
  staff_user_id bigint not null,
  status varchar(24) not null,
  start_at datetime not null,
  end_at datetime not null,
  reason varchar(400) null,
  created_by varchar(120) not null,
  updated_by varchar(120) not null,
  created_at datetime not null,
  updated_at datetime not null,
  primary key (id),
  key idx_rsa_merchant_booking (merchant_id, booking_id),
  key idx_rsa_merchant_staff_time (merchant_id, staff_user_id, start_at, end_at),
  constraint fk_rsa_merchant
    foreign key (merchant_id) references merchants (id),
  constraint fk_rsa_booking
    foreign key (booking_id) references bookings (id),
  constraint fk_rsa_resource
    foreign key (resource_id) references resource_items (id),
  constraint fk_rsa_staff_user
    foreign key (staff_user_id) references platform_users (id)
) engine=InnoDB default charset=utf8mb4;

