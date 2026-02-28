create table app_info (
  id bigserial primary key,
  created_at timestamp not null default now()
);