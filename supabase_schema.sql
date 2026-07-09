-- ObdScanner: esquema de telemetría
-- Pegar en Supabase Dashboard → SQL Editor → Run

create table if not exists trips (
  id text primary key,
  device text,
  start_ts bigint not null,          -- epoch ms
  end_ts bigint,                     -- epoch ms
  sample_count int default 0,
  created_at timestamptz default now()
);

create table if not exists telemetry (
  id bigint generated always as identity primary key,
  trip_id text references trips(id) on delete cascade,
  ts bigint not null,                -- epoch ms
  pid int not null,
  name text,
  value double precision,
  unit text
);

create index if not exists telemetry_trip_ts on telemetry (trip_id, ts);
create unique index if not exists telemetry_dedup on telemetry (trip_id, ts, pid);

-- Vista útil para análisis: timestamps legibles
create or replace view telemetry_readable as
select t.trip_id, to_timestamp(t.ts / 1000.0) as time, t.pid, t.name, t.value, t.unit
from telemetry t;

alter table trips enable row level security;
alter table telemetry enable row level security;

-- La app usa la clave publishable (rol anon): permitir insertar y leer
create policy "anon insert trips" on trips for insert to anon with check (true);
create policy "anon update trips" on trips for update to anon using (true);
create policy "anon read trips"   on trips for select to anon using (true);
create policy "anon insert telemetry" on telemetry for insert to anon with check (true);
create policy "anon read telemetry"   on telemetry for select to anon using (true);
