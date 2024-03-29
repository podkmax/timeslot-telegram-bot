--liquibase formatted sql
--changeset Podkorytov.Maksim:V_1.0.0_create_base_tables.sql

create table if not exists user_table
(
    user_id   bigint primary key not null,
    user_name text,
    active    bool,
    subscribe bool
);

create table if not exists user_session
(
    id                serial primary key not null,
    user_id           bigint,
    chat_id           bigint,
    last_message_id   bigint,
    last_message_text text,
    created_date_time timestamp,
    expired_date_time timestamp,
    session_state     text
);

create table if not exists access_request
(
    id      serial primary key not null,
    user_id bigint
);