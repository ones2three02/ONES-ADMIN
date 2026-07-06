create table if not exists sys_dict_type (
    id bigint auto_increment primary key,
    dict_code varchar(64) not null,
    dict_name varchar(128) not null,
    remark varchar(255),
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_dict_type_code unique (dict_code)
);

create index idx_sys_dict_type_sort
    on sys_dict_type (sort_order, id);

create table if not exists sys_dict_item (
    id bigint auto_increment primary key,
    type_id bigint not null,
    dict_code varchar(64) not null,
    item_label varchar(128) not null,
    item_value varchar(128) not null,
    color varchar(32),
    remark varchar(255),
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_dict_item_code_value unique (dict_code, item_value)
);

create index idx_sys_dict_item_type_sort
    on sys_dict_item (type_id, sort_order, id);

create index idx_sys_dict_item_code_enabled_sort
    on sys_dict_item (dict_code, enabled, sort_order, id);
