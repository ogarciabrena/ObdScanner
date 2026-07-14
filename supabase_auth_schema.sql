-- ObdScanner: migración a autenticación por usuario (Supabase Auth)
-- Pegar en Supabase Dashboard → SQL Editor → Run
-- (ejecutar DESPUÉS de supabase_schema.sql)

-- 1. Cada fila pertenece a un usuario autenticado
alter table trips     add column if not exists user_id uuid default auth.uid();
alter table telemetry add column if not exists user_id uuid default auth.uid();

-- 2. Quitar las políticas anónimas antiguas (acceso abierto)
drop policy if exists "anon insert trips"      on trips;
drop policy if exists "anon update trips"      on trips;
drop policy if exists "anon read trips"        on trips;
drop policy if exists "anon insert telemetry"  on telemetry;
drop policy if exists "anon read telemetry"    on telemetry;

-- 3. Cada usuario solo ve y escribe SUS datos
create policy "own trips insert" on trips
  for insert to authenticated with check (auth.uid() = user_id);
create policy "own trips select" on trips
  for select to authenticated using (auth.uid() = user_id);
create policy "own trips update" on trips
  for update to authenticated using (auth.uid() = user_id);

create policy "own telemetry insert" on telemetry
  for insert to authenticated with check (auth.uid() = user_id);
create policy "own telemetry select" on telemetry
  for select to authenticated using (auth.uid() = user_id);

-- 4. La vista legible hereda la seguridad de la tabla telemetry
--    (no requiere cambios)
