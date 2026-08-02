create table if not exists sys_api_manifest_snapshot (
    id bigint auto_increment primary key,
    application_version varchar(32) not null,
    checksum_algorithm varchar(32) not null,
    checksum varchar(128) not null,
    resource_count bigint not null,
    manifest_json text not null,
    publish_status varchar(32) not null,
    review_reason varchar(512),
    created_at timestamp not null default current_timestamp,
    constraint uk_sys_api_manifest_snapshot_version_checksum unique (application_version, checksum)
);

create index idx_sys_api_manifest_snapshot_created_at
    on sys_api_manifest_snapshot (created_at);

create index idx_sys_api_manifest_snapshot_checksum
    on sys_api_manifest_snapshot (checksum);
