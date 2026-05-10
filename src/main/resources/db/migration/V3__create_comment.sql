create table if not exists comment (
    id uuid primary key default gen_random_uuid(),
    content varchar(500) not null,
    article_id uuid not null references article(id) on delete cascade,
    author_id uuid not null references user_entity(id) on delete cascade,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);