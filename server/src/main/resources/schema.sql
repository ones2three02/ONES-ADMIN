create table if not exists sys_user (
    id bigint auto_increment primary key,
    username varchar(64) not null,
    display_name varchar(64) not null,
    dept_id bigint,
    avatar varchar(512),
    remark varchar(255),
    password_hash varchar(128) not null,
    enabled boolean not null default true,
    failed_login_count int not null default 0,
    locked_until timestamp null,
    last_login_at timestamp null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_user_username unique (username)
);

create table if not exists sys_role (
    id bigint auto_increment primary key,
    code varchar(64) not null,
    name varchar(64) not null,
    remark varchar(255),
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_role_code unique (code)
);

create table if not exists sys_permission (
    id bigint auto_increment primary key,
    code varchar(128) not null,
    name varchar(128) not null,
    created_at timestamp not null default current_timestamp,
    constraint uk_sys_permission_code unique (code)
);

create table if not exists sys_user_role (
    id bigint auto_increment primary key,
    user_id bigint not null,
    role_id bigint not null,
    created_at timestamp not null default current_timestamp,
    constraint uk_sys_user_role unique (user_id, role_id)
);

create table if not exists sys_role_permission (
    id bigint auto_increment primary key,
    role_id bigint not null,
    permission_id bigint not null,
    created_at timestamp not null default current_timestamp,
    constraint uk_sys_role_permission unique (role_id, permission_id)
);

create table if not exists sys_dept (
    id bigint auto_increment primary key,
    parent_id bigint,
    name varchar(64) not null,
    remark varchar(255),
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists sys_menu (
    id bigint auto_increment primary key,
    parent_id bigint,
    name varchar(64) not null,
    title varchar(128) not null,
    path varchar(255) not null,
    component varchar(255),
    redirect varchar(255),
    auth_code varchar(128),
    icon varchar(128),
    type varchar(32) not null default 'menu',
    sort_order int not null default 0,
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_sys_menu_name unique (name),
    constraint uk_sys_menu_path unique (path)
);
