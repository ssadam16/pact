create table if not exists chat (
    id uuid primary key default gen_random_uuid(),
    f_user_id uuid not null references user_entity(id) on delete cascade,
    s_user_id uuid not null references user_entity(id) on delete cascade,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists chat_message (
    id uuid primary key default gen_random_uuid(),
    content varchar(4096),
    author_id uuid not null references user_entity(id) on delete cascade,
    chat_id uuid not null references chat(id) on delete cascade,
    status varchar(50) not null,
    read_at timestamp,
    is_edited boolean not null,
    reply_to_message_id uuid references chat_message(id) on delete restrict,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists chat_media (
    id uuid primary key default gen_random_uuid(),
    media_type varchar(20) not null,
    message_id uuid not null references chat_message(id) on delete cascade,
    filename varchar(255) not null,
    file_url varchar(255) not null,
    order_num int not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists chat_read_receipt (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references user_entity(id) on delete cascade,
    message_id uuid not null references chat_message(id) on delete cascade,
    read_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
)