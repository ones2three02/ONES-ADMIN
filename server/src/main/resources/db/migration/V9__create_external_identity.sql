create table if not exists sys_external_identity (
    id bigint auto_increment primary key,
    provider varchar(32) not null,
    tenant_key varchar(128) not null,
    external_subject varchar(128) not null,
    user_id bigint not null,
    status varchar(32) not null default 'ACTIVE',
    last_login_at timestamp null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_external_identity_subject unique (provider, tenant_key, external_subject)
);

create index idx_sys_external_identity_user on sys_external_identity (user_id);

alter table sys_login_log add column auth_method varchar(32) not null default 'PASSWORD';
alter table sys_login_log add column provider varchar(32);
alter table sys_login_log add column external_identity_id bigint;

create index idx_sys_login_log_auth_method on sys_login_log (auth_method, provider);
