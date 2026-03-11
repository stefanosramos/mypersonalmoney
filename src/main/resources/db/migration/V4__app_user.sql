create table app_user (
    id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(200) not null,
    role varchar(20) not null,
    active boolean not null default true
);