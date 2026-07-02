create table if not exists hr_position (
    id bigint auto_increment primary key,
    position_code varchar(64) not null,
    position_name varchar(100) not null,
    dept_id bigint,
    description varchar(500),
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_hr_position_code unique (position_code)
);

create index idx_hr_position_dept
    on hr_position (dept_id);

create table if not exists hr_job_grade (
    id bigint auto_increment primary key,
    grade_code varchar(64) not null,
    grade_name varchar(100) not null,
    grade_rank int not null default 0,
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_hr_job_grade_code unique (grade_code)
);

create index idx_hr_job_grade_rank
    on hr_job_grade (grade_rank);

create table if not exists hr_employee (
    id bigint auto_increment primary key,
    employee_no varchar(64) not null,
    real_name varchar(100) not null,
    preferred_name varchar(100),
    gender varchar(20),
    mobile varchar(32),
    email varchar(128),
    id_card_masked varchar(64),
    id_card_encrypted varchar(255),
    user_id bigint,
    dept_id bigint not null,
    position_id bigint,
    grade_id bigint,
    manager_employee_id bigint,
    employment_type varchar(32) not null,
    employment_status varchar(32) not null,
    hire_date date not null,
    probation_end_date date,
    leave_date date,
    remark varchar(500),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_hr_employee_no unique (employee_no)
);

create index idx_hr_employee_dept_status
    on hr_employee (dept_id, employment_status);

create index idx_hr_employee_position
    on hr_employee (position_id);

create index idx_hr_employee_manager
    on hr_employee (manager_employee_id);

create index idx_hr_employee_hire_date
    on hr_employee (hire_date);

create index idx_hr_employee_user
    on hr_employee (user_id);

create table if not exists hr_employee_job (
    id bigint auto_increment primary key,
    employee_id bigint not null,
    dept_id bigint not null,
    position_id bigint,
    grade_id bigint,
    manager_employee_id bigint,
    employment_type varchar(32) not null,
    effective_date date not null,
    end_date date,
    change_reason varchar(255),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index idx_hr_employee_job_employee
    on hr_employee_job (employee_id, effective_date);

create index idx_hr_employee_job_dept
    on hr_employee_job (dept_id);

create table if not exists hr_employee_lifecycle_event (
    id bigint auto_increment primary key,
    employee_id bigint not null,
    event_type varchar(32) not null,
    event_date date not null,
    before_status varchar(32),
    after_status varchar(32),
    summary varchar(255) not null,
    detail_json text,
    created_by bigint,
    created_at timestamp not null default current_timestamp
);

create index idx_hr_lifecycle_employee
    on hr_employee_lifecycle_event (employee_id, event_date);

create index idx_hr_lifecycle_type
    on hr_employee_lifecycle_event (event_type);

create table if not exists hr_employee_contract (
    id bigint auto_increment primary key,
    employee_id bigint not null,
    contract_no varchar(64) not null,
    contract_type varchar(32) not null,
    status varchar(32) not null,
    start_date date not null,
    end_date date,
    probation_months int,
    renewal_remind_date date,
    attachment_file_id bigint,
    remark varchar(500),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_hr_contract_no unique (contract_no)
);

create index idx_hr_contract_employee
    on hr_employee_contract (employee_id);

create index idx_hr_contract_status_end_date
    on hr_employee_contract (status, end_date);

create table if not exists hr_roster_import_batch (
    id bigint auto_increment primary key,
    batch_no varchar(64) not null,
    file_name varchar(255) not null,
    status varchar(32) not null,
    total_count int not null default 0,
    success_count int not null default 0,
    failed_count int not null default 0,
    created_by bigint,
    created_at timestamp not null default current_timestamp,
    completed_at timestamp,
    constraint uk_hr_roster_import_batch_no unique (batch_no)
);

create index idx_hr_roster_import_created
    on hr_roster_import_batch (created_by, created_at);

create table if not exists hr_roster_import_error (
    id bigint auto_increment primary key,
    batch_id bigint not null,
    row_number int not null,
    employee_no varchar(64),
    field_name varchar(64) not null,
    error_message varchar(500) not null,
    raw_json text,
    created_at timestamp not null default current_timestamp
);

create index idx_hr_roster_error_batch
    on hr_roster_import_error (batch_id, row_number);
