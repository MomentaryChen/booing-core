-- Store arbitrary entity identifiers (UUID or legacy numeric) as opaque strings.
alter table audit_logs
  modify column target_id varchar(64) not null;
