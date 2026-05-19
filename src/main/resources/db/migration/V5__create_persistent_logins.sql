create table if not exists persistent_logins (
    series varchar(64) primary key,
    username varchar(64) not null,
    token varchar(64) not null,
    last_used timestamp not null
);

create index idx_persistent_logins_username on persistent_logins(username);