create table if not exists article (
    id uuid primary key default gen_random_uuid(),
    title varchar(255) not null,
    content text not null,
    author_id uuid not null references user_entity(id) on delete cascade,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists game (
    id uuid primary key default gen_random_uuid(),
    name varchar(255) not null,
    developer varchar(255) not null,
    steam_link varchar(500),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists article_game (
    article_id uuid not null references article(id) on delete cascade,
    game_id uuid not null references game(id) on delete cascade,
    primary key (article_id, game_id)
);

create table if not exists article_tag (
    id uuid primary key default gen_random_uuid(),
    name varchar(50) not null unique,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists article_tag_article (
    tag_id uuid not null references article_tag(id) on delete cascade,
    article_id uuid not null references article(id) on delete cascade,
    primary key (tag_id, article_id)
);

create table if not exists article_like (
    user_id uuid not null references user_entity(id) on delete cascade,
    article_id uuid not null references article(id) on delete cascade,
    primary key (user_id, article_id)
);