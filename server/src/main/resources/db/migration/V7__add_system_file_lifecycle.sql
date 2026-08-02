alter table sys_file
    add column status varchar(32) not null default 'ACTIVE';

alter table sys_file
    add column deleted_at timestamp null;

create index idx_sys_file_status_created
    on sys_file (status, created_at);
