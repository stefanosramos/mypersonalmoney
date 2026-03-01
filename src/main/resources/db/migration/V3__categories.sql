-- Categories / Subcategories (MS Money style)
-- PostgreSQL

-- 1) Category
create table if not exists category (
  id          bigserial primary key,
  name        varchar(120) not null,
  type        varchar(10)  not null, -- INCOME / EXPENSE
  active      boolean      not null default true,
  created_at  timestamptz  not null default now(),

  constraint ck_category_type check (type in ('INCOME','EXPENSE'))
);

-- names per type should be unique (allow same name in INCOME and EXPENSE)
create unique index if not exists ux_category_name_type on category(lower(name), type);
create index if not exists ix_category_active on category(active);
create index if not exists ix_category_type on category(type);


-- 2) SubCategory
create table if not exists subcategory (
  id           bigserial primary key,
  category_id  bigint      not null,
  name         varchar(120) not null,
  active       boolean     not null default true,
  created_at   timestamptz not null default now(),

  constraint fk_subcategory_category
    foreign key (category_id) references category(id) on delete restrict
);

-- subcategory name unique within its parent category
create unique index if not exists ux_subcategory_cat_name on subcategory(category_id, lower(name));
create index if not exists ix_subcategory_active on subcategory(active);
create index if not exists ix_subcategory_category_id on subcategory(category_id);


-- 3) Link categories to ledger entries
alter table ledger_entry
  add column if not exists category_id bigint null;

alter table ledger_entry
  add column if not exists subcategory_id bigint null;

alter table ledger_entry
  add constraint fk_entry_category
    foreign key (category_id) references category(id);

alter table ledger_entry
  add constraint fk_entry_subcategory
    foreign key (subcategory_id) references subcategory(id);

create index if not exists ix_entry_category_id on ledger_entry(category_id);
create index if not exists ix_entry_subcategory_id on ledger_entry(subcategory_id);