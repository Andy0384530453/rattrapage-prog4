create table if not exists submission
(
    id            varchar
        constraint submission_pk primary key,
    email         varchar not null,
    thumbnail_key varchar,
    created_at    timestamp with time zone not null
);