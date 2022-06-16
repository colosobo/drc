-- auto-generated definition
create table drc_db_config
(
    id            int auto_increment
        primary key,
    url           varchar(255)                       null,
    username      varchar(255)                       null,
    password      varchar(255)                       null,
    database_name varchar(255)                       null,
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null,
    is_deleted    int      default 0                 not null
);

-- auto-generated definition
create table drc_machine_register_table
(
    id          bigint auto_increment
        primary key,
    ip_port     varchar(63)                        null,
    type        int                                null,
    ext_info    text                               null,
    update_time datetime default CURRENT_TIMESTAMP null,
    create_time datetime default CURRENT_TIMESTAMP null,
    is_deleted  int      default 0                 null,
    constraint drc_machine_register_table_id_uindex
        unique (id),
    constraint drc_machine_register_table_ip_port_uindex
        unique (ip_port)
);

-- auto-generated definition
create table drc_qps_log
(
    id              int auto_increment
        primary key,
    name            varchar(255)                       null,
    time_in_seconds bigint   default 0                 null,
    qps             int      default 0                 null,
    create_time     datetime default CURRENT_TIMESTAMP null,
    update_time     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    is_deleted      int      default 0                 null
);

create index time_in_seconds_indx
    on drc_qps_log (time_in_seconds);

-- auto-generated definition
create table drc_sub_task_full_config
(
    id                         int auto_increment
        primary key,
    drc_task_id                int                                null,
    range_size_config          int      default 1000000           null,
    db_config_id               int                                null,
    table_name                 varchar(255)                       null,
    select_field_list          varchar(255)                       null,
    split_state                int      default 0                 null,
    slice_count                int      default 0                 not null,
    finish_slice_count         int      default 0                 not null,
    sink_json                  text                               null,
    one_slice_qps_limit_config int      default 1000              not null,
    create_time                datetime default CURRENT_TIMESTAMP null,
    update_time                datetime default CURRENT_TIMESTAMP null,
    is_deleted                 int      default 0                 null,
    where_statement            varchar(256)                       null
);

-- auto-generated definition
create table drc_sub_task_full_slice_detail
(
    id                          int auto_increment
        primary key,
    parent_id                   int                                null,
    sub_task_name               varchar(111)                       null,
    slice_number                int      default 0                 null,
    slice_pk_name               varchar(255)                       null,
    slice_min_pk                varchar(255)                       null,
    slice_max_pk                varchar(255)                       null,
    range_size                  int                                null,
    is_last_slice               int      default 0                 null,
    slice_cursor                varchar(255)                       null,
    state                       int      default 0                 null,
    drc_sub_task_full_config_id int                                null,
    create_time                 datetime default CURRENT_TIMESTAMP not null,
    update_time                 datetime default CURRENT_TIMESTAMP not null,
    is_deleted                  int      default 0                 not null,
    constraint sub_task_name_indx
        unique (sub_task_name)
);

create index drc_sub_task_full_slice_detail_parent_id_index
    on drc_sub_task_full_slice_detail (parent_id);

-- auto-generated definition
create table drc_sub_task_incr
(
    id                         int auto_increment
        primary key,
    sub_task_name              varchar(191)                           null,
    parent_id                  int                                    null,
    db_config_id               int                                    null,
    table_expression           text                                   null,
    one_slice_qps_limit_config int          default 1000              not null,
    sink_json                  text                                   null,
    create_time                datetime     default CURRENT_TIMESTAMP not null,
    update_time                datetime     default CURRENT_TIMESTAMP not null,
    is_deleted                 varchar(255) default '0'               not null,
    state                      int          default 0                 null,
    ext                        text                                   null,
    constraint drc_sub_task_incr_sub_task_name_uindex
        unique (sub_task_name)
);

-- auto-generated definition
create table drc_sub_task_schema_log
(
    id                 int auto_increment
        primary key,
    parent_id          int                                 not null,
    table_total        int       default 0                 not null comment '表的数量',
    split_finish       int       default 0                 not null comment '同步完成了几张表',
    table_expression   varchar(255)                        null comment '表过滤表达式',
    table_split_finish int                                 null comment '表拆分数量',
    table_list         text                                null comment '表集合，逗号分隔',
    create_time        timestamp default CURRENT_TIMESTAMP null,
    update_time        timestamp default CURRENT_TIMESTAMP null,
    is_deleted         int       default 0                 null
);

-- auto-generated definition
create table drc_task
(
    id               int auto_increment
        primary key,
    task_name        varchar(128)                       not null,
    task_desc        varchar(255)                       null,
    state            int      default 0                 null comment '状态 0初始化，1启动，2暂停，3停止，4完成。',
    task_type        int                                null comment '任务类型1增量，2全量，3增量+全量',
    sink_json        text                               null,
    user_alias       varchar(255)                       null,
    qps_limit_config int      default 1000              null,
    create_time      datetime default CURRENT_TIMESTAMP not null,
    update_time      datetime default CURRENT_TIMESTAMP not null,
    is_deleted       int      default 0                 not null,
    constraint task_name
        unique (task_name)
);

-- auto-generated definition
create table drc_task_register_table
(
    id             bigint auto_increment
        primary key,
    worker_id      bigint                             null,
    task_name      varchar(128)                       null,
    worker_ip_port varchar(255)                       null,
    ext            text                               null,
    create_time    datetime default CURRENT_TIMESTAMP null,
    update_time    datetime default CURRENT_TIMESTAMP null,
    is_deleted     int      default 0                 null,
    constraint drc_task_register_table_task_name_index
        unique (task_name),
    constraint task_register_table_id_uindex
        unique (id)
);

