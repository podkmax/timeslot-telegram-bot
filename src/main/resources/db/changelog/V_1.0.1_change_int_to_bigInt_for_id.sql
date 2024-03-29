--liquibase formatted sql
--changeset Podkorytov.Maksim:V_1.0.1_change_int_to_bigInt_for_id.sql

alter table user_table alter column user_id set data type bigint;
alter table user_session alter column user_id set data type bigint;
alter table user_session alter column chat_id set data type bigint;
alter table access_request alter column user_id set data type bigint;