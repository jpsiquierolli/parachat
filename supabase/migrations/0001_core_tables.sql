-- Core tables for Parachat (Firebase auth + Supabase data/storage)

create extension if not exists pgcrypto;

create table if not exists public.users (
  id text primary key,
  email text not null unique,
  username text,
  photo_url text,
  status text not null default 'OFFLINE',
  about text not null default '',
  last_seen bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.conversations (
  id text primary key,
  owner_id text not null references public.users(id) on delete cascade,
  other_user_id text not null,
  title text not null default '',
  last_message_preview text not null default '',
  last_message_timestamp bigint not null default 0,
  unread_count integer not null default 0,
  is_group boolean not null default false,
  participants jsonb not null default '[]'::jsonb,
  pinned_message_id text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(owner_id, other_user_id)
);

create table if not exists public.messages (
  id text primary key,
  sender_id text not null references public.users(id) on delete cascade,
  receiver_id text not null references public.users(id) on delete cascade,
  content text not null default '',
  media_url text,
  media_thumbnail_url text,
  media_duration_millis bigint,
  latitude double precision,
  longitude double precision,
  type text not null default 'TEXT',
  status text not null default 'SENT',
  timestamp bigint not null default ((extract(epoch from now()) * 1000)::bigint),
  conversation_id text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.groups (
  id text primary key,
  name text not null,
  description text not null default '',
  photo_url text,
  creator_id text not null references public.users(id) on delete cascade,
  members jsonb not null default '[]'::jsonb,
  created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

create index if not exists idx_users_username on public.users (username);
create index if not exists idx_conversations_owner_last_ts on public.conversations (owner_id, last_message_timestamp desc);
create index if not exists idx_messages_conversation_ts on public.messages (conversation_id, timestamp asc);
create index if not exists idx_groups_members on public.groups using gin (members);

alter table public.users enable row level security;
alter table public.conversations enable row level security;
alter table public.messages enable row level security;
alter table public.groups enable row level security;

-- Demo/class setup: allow anon and authenticated app traffic.
-- Tighten these policies before production.
drop policy if exists users_select_all on public.users;
create policy users_select_all on public.users for select to anon, authenticated using (true);

drop policy if exists users_insert_all on public.users;
create policy users_insert_all on public.users for insert to anon, authenticated with check (true);

drop policy if exists users_update_all on public.users;
create policy users_update_all on public.users for update to anon, authenticated using (true) with check (true);

drop policy if exists conversations_all on public.conversations;
create policy conversations_all on public.conversations for all to anon, authenticated using (true) with check (true);

drop policy if exists messages_all on public.messages;
create policy messages_all on public.messages for all to anon, authenticated using (true) with check (true);

drop policy if exists groups_all on public.groups;
create policy groups_all on public.groups for all to anon, authenticated using (true) with check (true);

