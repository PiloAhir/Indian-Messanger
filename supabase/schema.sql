-- Indian Messenger starter schema.
-- Run in Supabase SQL Editor. Never expose service_role keys in the app.
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  user_id text unique not null,
  gender text,
  birth_date date,
  public_key text,
  created_at timestamptz not null default now(),
  constraint user_id_format check (
    length(user_id) >= 12
    and user_id ~ '[A-Z]'
    and user_id ~ '[a-z]'
    and user_id ~ '[0-9]'
    and user_id ~ '[^A-Za-z0-9]'
  )
);
alter table public.profiles enable row level security;
-- Exact-ID search is exposed to signed-in users; restrict fields returned by app.
create policy "authenticated can search profiles" on public.profiles
for select to authenticated using (true);
create policy "users can insert own profile" on public.profiles
for insert to authenticated with check (auth.uid() = id);
create policy "users can update own profile" on public.profiles
for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  sender_id uuid not null references auth.users(id) on delete cascade,
  recipient_id uuid not null references auth.users(id) on delete cascade,
  ciphertext text not null,
  created_at timestamptz not null default now()
);
alter table public.messages enable row level security;
create policy "participants read own messages" on public.messages
for select to authenticated
using (auth.uid() = sender_id or auth.uid() = recipient_id);
create policy "sender inserts own messages" on public.messages
for insert to authenticated
with check (auth.uid() = sender_id);
create index if not exists messages_pair_time on public.messages(sender_id, recipient_id, created_at desc);
