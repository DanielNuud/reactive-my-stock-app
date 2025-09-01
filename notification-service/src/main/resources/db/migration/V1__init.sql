create table if not exists public.notifications (
                                                    id            bigserial primary key,
                                                    user_key      text        not null,
                                                    title         text        not null,
                                                    message       text        not null,
                                                    level         text        not null default 'INFO',
                                                    read_flag     boolean     not null default false,
                                                    created_at    timestamptz not null default now(),
    dedupe_key    text        not null,

    constraint ux_notifications_dedupe_key unique (dedupe_key),
    constraint chk_notifications_level check (level in ('INFO','WARNING','ERROR'))
    );


create index if not exists ix_notifications_user_created_at
    on public.notifications (user_key, created_at desc);