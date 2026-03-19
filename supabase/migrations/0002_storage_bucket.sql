-- Storage bucket + policies for chat media and profile photos

insert into storage.buckets (id, name, public)
values ('chat-media', 'chat-media', true)
on conflict (id) do update set public = excluded.public;

-- Demo/class setup: allow app uploads/downloads with anon key.
drop policy if exists media_select_all on storage.objects;
create policy media_select_all
on storage.objects
for select
to anon, authenticated
using (bucket_id = 'chat-media');

drop policy if exists media_insert_all on storage.objects;
create policy media_insert_all
on storage.objects
for insert
to anon, authenticated
with check (bucket_id = 'chat-media');

drop policy if exists media_update_all on storage.objects;
create policy media_update_all
on storage.objects
for update
to anon, authenticated
using (bucket_id = 'chat-media')
with check (bucket_id = 'chat-media');

drop policy if exists media_delete_all on storage.objects;
create policy media_delete_all
on storage.objects
for delete
to anon, authenticated
using (bucket_id = 'chat-media');

