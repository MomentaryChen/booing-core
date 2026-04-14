-- Link client-created bookings to platform user for "my bookings" queries.
alter table bookings
  add column platform_user_id bigint null;

create index idx_bookings_platform_user_id on bookings (platform_user_id);

alter table bookings
  add constraint fk_bookings_platform_user
    foreign key (platform_user_id) references platform_users (id);
