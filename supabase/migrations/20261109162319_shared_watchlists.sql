-- Public RPCs expose app operations; privileged implementations stay outside the Data API schema.
create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;
create schema if not exists movers_private;
revoke all on schema movers_private from public;
grant usage on schema movers_private to authenticated;

create table movers_private.profiles (
    id uuid primary key references auth.users(id),
    name text not null check (length(btrim(name)) between 1 and 80),
    color integer not null check (color between 0 and 7)
);
create table movers_private.lists (
    id uuid primary key,
    name text not null check (length(btrim(name)) between 1 and 200),
    creator uuid not null references movers_private.profiles(id),
    created_at bigint not null,
    updated_at bigint not null,
    revision bigint not null default 1,
    retired boolean not null default false
);
create index lists_creator on movers_private.lists(creator);
create table movers_private.members (
    list_id uuid not null references movers_private.lists(id),
    person_id uuid not null references movers_private.profiles(id),
    active boolean not null default true,
    primary key (list_id, person_id)
);
create index members_person on movers_private.members(person_id, active, list_id);
create function movers_private.valid_film(p_film jsonb) returns boolean
language sql immutable set search_path = '' as $$
    select coalesce(jsonb_typeof(p_film)='object'
        and jsonb_typeof(p_film->'id')='number' and (p_film->>'id')::numeric>0
        and jsonb_typeof(p_film->'title')='string' and length(btrim(p_film->>'title')) between 1 and 500
        and jsonb_typeof(p_film->'synopsis')='string' and length(p_film->>'synopsis')<=20000
        and jsonb_typeof(p_film->'releaseDate')='string' and length(p_film->>'releaseDate')<=64
        and (p_film->>'posterUrl' is null or (jsonb_typeof(p_film->'posterUrl')='string'
            and length(p_film->>'posterUrl')<=2048 and p_film->>'posterUrl' like 'https://image.tmdb.org/t/p/%')),false)
$$;
create table movers_private.entries (
    list_id uuid not null references movers_private.lists(id),
    movie_id integer not null check (movie_id > 0),
    film jsonb not null check (movers_private.valid_film(film)),
    actor uuid not null references movers_private.profiles(id),
    added_at bigint not null,
    imported boolean not null default false,
    primary key (list_id, movie_id)
);
create index entries_actor on movers_private.entries(actor);
create table movers_private.activity (
    id uuid primary key default extensions.gen_random_uuid(),
    sequence bigint generated always as identity unique,
    list_id uuid not null references movers_private.lists(id),
    actor uuid not null references movers_private.profiles(id),
    actor_snapshot jsonb not null,
    film jsonb not null check (movers_private.valid_film(film)),
    added boolean not null,
    at bigint not null,
    imported boolean not null default false
);
create index activity_list_sequence on movers_private.activity(list_id, sequence desc);
create index activity_actor on movers_private.activity(actor);
create table movers_private.invites (
    token_hash bytea primary key,
    list_id uuid not null references movers_private.lists(id),
    expires_at timestamptz not null,
    revoked boolean not null default false
);
create index invites_list on movers_private.invites(list_id);
create table movers_private.receipts (
    person_id uuid not null references movers_private.profiles(id),
    request_id uuid not null,
    command_hash bytea not null,
    primary key(person_id, request_id)
);

create table public.movers_list_signals (
    id uuid primary key references movers_private.lists(id),
    revision bigint not null
);
alter table public.movers_list_signals enable row level security;
revoke all on public.movers_list_signals from public, anon, authenticated;
grant select on public.movers_list_signals to authenticated;
revoke all on all tables in schema movers_private from public, anon, authenticated;
do $$ declare t record; begin
    for t in select tablename from pg_tables where schemaname = 'movers_private' loop
        execute format('alter table movers_private.%I enable row level security', t.tablename);
    end loop;
end $$;

create function movers_private.member(p_list uuid) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists(select 1 from movers_private.members m join movers_private.lists l on l.id=m.list_id
        where m.list_id=p_list and m.person_id=(select auth.uid()) and m.active and not l.retired)
$$;
create policy signals_member on public.movers_list_signals for select to authenticated
    using ((select movers_private.member(id)));

create function movers_private.person(p_id uuid) returns jsonb
language sql stable security definer set search_path = '' as $$
    select jsonb_build_object('id', id, 'name', name, 'color', color) from movers_private.profiles where id=p_id
$$;
create function movers_private.now_ms() returns bigint
language sql stable set search_path = '' as $$ select (extract(epoch from now())*1000)::bigint $$;
create function movers_private.touch(p_list uuid) returns void
language plpgsql security definer set search_path = '' as $$ begin
    update movers_private.lists set revision=revision+1 where id=p_list;
    insert into public.movers_list_signals select id,revision from movers_private.lists where id=p_list
        on conflict(id) do update set revision=excluded.revision;
end $$;

create function movers_private.profile(p_name text, p_color integer) returns void
language plpgsql security definer set search_path = '' as $$
declare v_list uuid; begin
    if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
    insert into movers_private.profiles values(auth.uid(), btrim(p_name), p_color)
        on conflict(id) do update set name=excluded.name, color=excluded.color
        where movers_private.profiles.name<>excluded.name or movers_private.profiles.color<>excluded.color;
    if not found then return; end if;
    for v_list in select list_id from movers_private.members where person_id=auth.uid() and active order by list_id loop
        perform movers_private.touch(v_list);
    end loop;
end $$;

create function movers_private.snapshot(p_list uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb; begin
    if not movers_private.member(p_list) then raise exception 'Access denied' using errcode='42501'; end if;
    select jsonb_build_object('id',l.id,'name',l.name,'createdAt',l.created_at,
        'info',jsonb_build_object('creator',movers_private.person(l.creator),'updatedAt',l.updated_at,'revision',l.revision,
            'members',coalesce((select jsonb_agg(movers_private.person(m.person_id) order by m.person_id)
                from movers_private.members m where m.list_id=l.id and m.active),'[]'::jsonb)),
        'entries',coalesce((select jsonb_agg(jsonb_build_object('film',e.film,'contribution',jsonb_build_object(
            'movieId',e.movie_id,'actor',movers_private.person(e.actor),'addedAt',e.added_at,'imported',e.imported))
            order by e.added_at desc,e.movie_id desc) from movers_private.entries e where e.list_id=l.id),'[]'::jsonb))
    into v_result from movers_private.lists l where l.id=p_list;
    return v_result;
end $$;

create function movers_private.preview(p_token text) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb; begin
    if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
    select jsonb_build_object('listId',l.id,'name',l.name,'creator',movers_private.person(l.creator),
        'memberCount',(select count(*) from movers_private.members where list_id=l.id and active),
        'movieCount',(select count(*) from movers_private.entries where list_id=l.id)) into v_result
    from movers_private.invites i join movers_private.lists l on l.id=i.list_id
    where i.token_hash=extensions.digest(p_token,'sha256') and i.expires_at>now() and not i.revoked and not l.retired;
    if v_result is null then raise exception 'This invitation is invalid, expired or revoked.' using errcode='22023'; end if;
    return v_result;
end $$;

create function movers_private.command(p_request uuid, p_command jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_actor uuid := auth.uid(); v_list uuid := (p_command->>'listId')::uuid;
    v_kind text := split_part(p_command->>'type','.',-1);
    v_film jsonb; v_entry movers_private.entries; v_id integer; v_changed boolean;
    v_hash bytea := extensions.digest(p_command::text,'sha256'); v_existing bytea;
    v_at bigint := movers_private.now_ms(); v_preview jsonb;
begin
    if v_actor is null then raise exception 'Authentication required' using errcode='42501'; end if;
    perform pg_advisory_xact_lock(hashtextextended(v_actor::text || p_request::text,0));
    select command_hash into v_existing from movers_private.receipts where person_id=v_actor and request_id=p_request;
    if v_existing is not null then
        if v_existing<>v_hash then raise exception 'Request ID reused for a different command' using errcode='22023'; end if;
        return;
    end if;
    if not exists(select 1 from movers_private.profiles where id=v_actor) then raise exception 'Set your name first' using errcode='42501'; end if;
    if v_kind='Publish' then
        if jsonb_array_length(p_command->'films')>10000 then raise exception 'List too large' using errcode='22023'; end if;
        insert into movers_private.lists(id,name,creator,created_at,updated_at)
            values(v_list,btrim(p_command->>'name'),v_actor,v_at,v_at);
        insert into movers_private.members values(v_list,v_actor,true);
        for v_film in select value from jsonb_array_elements(p_command->'films') loop
            v_id := (v_film->>'id')::integer;
            insert into movers_private.entries values(v_list,v_id,v_film,v_actor,v_at,true);
            insert into movers_private.activity(list_id,actor,actor_snapshot,film,added,at,imported)
                values(v_list,v_actor,movers_private.person(v_actor),v_film,true,v_at,true);
        end loop;
    else
        perform 1 from movers_private.lists where id=v_list and not retired for update;
        if not found then raise exception 'Access denied' using errcode='42501'; end if;
        if v_kind='Join' then
            v_preview := movers_private.preview(p_command->>'token');
            if (v_preview->>'listId')::uuid<>v_list then raise exception 'Invalid invitation' using errcode='22023'; end if;
            insert into movers_private.members values(v_list,v_actor,true)
                on conflict(list_id,person_id) do update set active=true;
        else
            if not movers_private.member(v_list) then raise exception 'Access denied' using errcode='42501'; end if;
            case v_kind
            when 'SetFilm' then
                v_film := p_command->'film'; v_id := (v_film->>'id')::integer;
                if (p_command->>'present')::boolean then
                    insert into movers_private.entries values(v_list,v_id,v_film,v_actor,v_at,false) on conflict do nothing;
                    v_changed := found;
                    if v_changed then
                        insert into movers_private.activity(list_id,actor,actor_snapshot,film,added,at)
                            values(v_list,v_actor,movers_private.person(v_actor),v_film,true,v_at);
                    end if;
                else
                    delete from movers_private.entries where list_id=v_list and movie_id=v_id returning * into v_entry;
                    v_changed := found;
                    if v_changed then
                        insert into movers_private.activity(list_id,actor,actor_snapshot,film,added,at)
                            values(v_list,v_actor,movers_private.person(v_actor),v_entry.film,false,v_at);
                    end if;
                end if;
                if v_changed then update movers_private.lists set updated_at=v_at where id=v_list; end if;
            when 'Rename' then
                update movers_private.lists set name=btrim(p_command->>'name'),updated_at=v_at where id=v_list;
            when 'Leave' then
                update movers_private.members set active=false where list_id=v_list and person_id=v_actor;
                if not exists(select 1 from movers_private.members where list_id=v_list and active) then
                    update movers_private.lists set retired=true where id=v_list;
                    update movers_private.invites set revoked=true where list_id=v_list;
                end if;
            when 'RevokeInvites' then
                update movers_private.invites set revoked=true where list_id=v_list;
            else raise exception 'Unsupported command' using errcode='22023';
            end case;
        end if;
    end if;
    perform movers_private.touch(v_list);
    insert into movers_private.receipts values(v_actor,p_request,v_hash);
end $$;

create function movers_private.invite(p_list uuid) returns text
language plpgsql security definer set search_path = '' as $$
declare v_token text; begin
    perform 1 from movers_private.lists where id=p_list for update;
    if not movers_private.member(p_list) then raise exception 'Access denied' using errcode='42501'; end if;
    v_token := encode(extensions.gen_random_bytes(32),'hex');
    insert into movers_private.invites values(extensions.digest(v_token,'sha256'),p_list,now()+interval '7 days',false);
    return v_token;
end $$;

create function movers_private.activity_page(p_list uuid, p_before bigint) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_events jsonb; v_next bigint; begin
    if not movers_private.member(p_list) then raise exception 'Access denied' using errcode='42501'; end if;
    select jsonb_agg(jsonb_build_object('id',id,'sequence',sequence,'actor',actor_snapshot,'film',film,'added',added,'at',at,'imported',imported) order by sequence desc),min(sequence)
        into v_events,v_next from (select * from movers_private.activity where list_id=p_list and (p_before is null or sequence<p_before) order by sequence desc limit 50) page;
    return jsonb_build_object('events',coalesce(v_events,'[]'::jsonb),'nextBefore',case when exists(select 1 from movers_private.activity where list_id=p_list and sequence<v_next) then v_next else null end);
end $$;

create function public.movers_profile(p_name text, p_color integer) returns void
language sql security invoker set search_path = '' as $$ select movers_private.profile(p_name,p_color) $$;
create function public.movers_snapshot(p_list uuid) returns jsonb
language sql stable security invoker set search_path = '' as $$ select movers_private.snapshot(p_list) $$;
create function public.movers_preview(p_token text) returns jsonb
language sql stable security invoker set search_path = '' as $$ select movers_private.preview(p_token) $$;
create function public.movers_command(p_request uuid, p_command jsonb) returns void
language sql security invoker set search_path = '' as $$ select movers_private.command(p_request,p_command) $$;
create function public.movers_invite(p_list uuid) returns text
language sql security invoker set search_path = '' as $$ select movers_private.invite(p_list) $$;
create function public.movers_activity(p_list uuid, p_before bigint default null) returns jsonb
language sql stable security invoker set search_path = '' as $$ select movers_private.activity_page(p_list,p_before) $$;
create function movers_private.list_ids() returns jsonb
language sql stable security definer set search_path = '' as $$
    select coalesce(jsonb_agg(m.list_id order by m.list_id),'[]'::jsonb) from movers_private.members m
        join movers_private.lists l on l.id=m.list_id where m.person_id=auth.uid() and m.active and not l.retired
$$;
create function public.movers_list_ids() returns jsonb
language sql stable security invoker set search_path = '' as $$ select movers_private.list_ids() $$;

-- Helpers are not callable by clients; only operation entrypoints and the RLS predicate are granted.
do $$ declare f record; begin
    for f in select p.oid::regprocedure as signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='movers_private' or (n.nspname='public' and p.proname like 'movers_%') loop
        execute format('revoke all on function %s from public, anon, authenticated',f.signature);
    end loop;
end $$;
grant execute on function movers_private.member(uuid), movers_private.profile(text,integer), movers_private.snapshot(uuid),
    movers_private.preview(text), movers_private.command(uuid,jsonb), movers_private.invite(uuid),
    movers_private.activity_page(uuid,bigint), movers_private.list_ids() to authenticated;
grant execute on function public.movers_profile(text,integer),public.movers_snapshot(uuid),public.movers_preview(text),
    public.movers_command(uuid,jsonb),public.movers_invite(uuid),public.movers_activity(uuid,bigint),public.movers_list_ids() to authenticated;

do $$ begin
    if exists(select 1 from pg_publication where pubname='supabase_realtime') then
        alter publication supabase_realtime add table public.movers_list_signals;
    end if;
end $$;
