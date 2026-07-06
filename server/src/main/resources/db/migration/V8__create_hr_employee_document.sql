create table if not exists hr_employee_document (
    id bigint auto_increment primary key,
    employee_id bigint not null,
    file_id bigint not null,
    document_type varchar(64) not null,
    issue_date date,
    expire_date date,
    remark varchar(500),
    created_by bigint,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_hr_employee_document_file unique (file_id)
);

create index idx_hr_employee_document_employee
    on hr_employee_document (employee_id, created_at);

create index idx_hr_employee_document_expire
    on hr_employee_document (expire_date);
