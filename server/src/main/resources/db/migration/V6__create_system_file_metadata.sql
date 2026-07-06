create table if not exists sys_file (
    id bigint auto_increment primary key,
    original_name varchar(255) not null,
    stored_name varchar(255) not null,
    url varchar(512) not null,
    content_type varchar(128),
    extension varchar(32) not null,
    size_bytes bigint not null,
    storage_type varchar(32) not null,
    bucket varchar(128),
    business_type varchar(64),
    business_id varchar(128),
    uploaded_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_file_stored_name unique (stored_name)
);

create index idx_sys_file_uploaded_by
    on sys_file (uploaded_by, created_at);

create index idx_sys_file_business
    on sys_file (business_type, business_id);

create index idx_sys_file_created_at
    on sys_file (created_at);
