alter table sys_role
    add column data_scope varchar(32) not null default 'DEPT_AND_CHILD';

update sys_role
set data_scope = 'ALL'
where code = 'SUPER_ADMIN';

create index idx_sys_role_data_scope
    on sys_role (data_scope);
