-- Contacts table for user-managed contact list

create table if not exists public.contacts (
  owner_id text not null references public.users(id) on delete cascade,
  contact_user_id text not null references public.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (owner_id, contact_user_id),
  check (owner_id <> contact_user_id)
);

create index if not exists idx_contacts_owner on public.contacts (owner_id);

alter table public.contacts enable row level security;

drop policy if exists contacts_all on public.contacts;
create policy contacts_all on public.contacts for all to anon, authenticated using (true) with check (true);

