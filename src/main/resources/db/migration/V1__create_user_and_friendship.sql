create table if not exists user_entity (
    id uuid primary key default gen_random_uuid(),
    username varchar(50) not null unique,
    name varchar(50) not null,
    email varchar(255) not null unique,
    hash_password varchar(255),
    role varchar(50) not null check (role in('USER', 'MODER', 'ADMIN')),
    auth_provider varchar(50) not null check (auth_provider in('LOCAL', 'GOOGLE', 'GITHUB', 'VK', 'YANDEX')),
    provider_id varchar(255),
    avatar_filename varchar(255),
    steam_id varchar(50) unique,
    is_enabled boolean default true,
    is_verified boolean default false,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists friendship (
    who_id uuid not null references user_entity(id) on delete cascade,
    to_whom_id uuid not null references user_entity(id) on delete cascade,
    status varchar(50) not null check (status in('PENDING', 'ACCEPTED')),
    primary key (who_id, to_whom_id)
);