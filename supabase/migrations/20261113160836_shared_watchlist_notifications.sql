-- Notification work commits with film activity, independently of either app's lifetime.
create table movers_private.push_settings (
    singleton boolean primary key default true check (singleton),
    enabled boolean not null default false
);
insert into movers_private.push_settings default values;
create table movers_private.push_devices (
    id uuid not null,
    environment text not null check (environment in ('sandbox','production')),
    person_id uuid not null references movers_private.profiles(id),
    token text not null check (length(token) between 32 and 512 and token ~ '^[0-9a-f]+$'),
    enabled boolean not null,
    registered_at timestamptz not null default clock_timestamp(),
    primary key (id,environment),
    unique (environment,token)
);
create index push_devices_person on movers_private.push_devices(person_id) where enabled;
create table movers_private.push_batches (
    id uuid primary key default extensions.gen_random_uuid(),
    list_id uuid not null references movers_private.lists(id),
    actor uuid not null references movers_private.profiles(id),
    actor_name text not null,
    due_at timestamptz not null,
    frozen boolean not null default false
);
create unique index push_batches_pending on movers_private.push_batches(list_id,actor) where not frozen;
create index push_batches_due on movers_private.push_batches(due_at) where not frozen;
create table movers_private.push_items (
    event_id uuid primary key references movers_private.activity(id),
    batch_id uuid not null references movers_private.push_batches(id) on delete cascade,
    movie_id integer not null,
    title text not null,
    recipients uuid[] not null,
    cancelled boolean not null default false
);
create index push_items_batch on movers_private.push_items(batch_id);
create index push_items_movie on movers_private.push_items(movie_id) where not cancelled;
create table movers_private.push_deliveries (
    id uuid primary key default extensions.gen_random_uuid(),
    batch_id uuid not null references movers_private.push_batches(id) on delete cascade,
    device_id uuid not null,
    environment text not null,
    person_id uuid not null references movers_private.profiles(id),
    token text not null,
    content jsonb not null,
    expires_at timestamptz not null,
    state text not null default 'ready' check (state in ('ready','leased','sent','dropped','failed')),
    attempts integer not null default 0,
    next_attempt_at timestamptz not null default now(),
    lease_id uuid,
    lease_until timestamptz,
    last_error text,
    unique (batch_id,device_id,environment),
    foreign key (device_id,environment) references movers_private.push_devices(id,environment) on delete cascade
);
create index push_deliveries_ready on movers_private.push_deliveries(environment,next_attempt_at)
    where state in ('ready','leased');
create table movers_private.push_provider_tokens (
    environment text primary key check (environment in ('sandbox','production')),
    key_id text not null,
    token text not null,
    issued_at timestamptz not null
);

do $$ declare v_table text; begin
    foreach v_table in array array['push_settings','push_devices','push_batches','push_items',
        'push_deliveries','push_provider_tokens'] loop
        execute format('alter table movers_private.%I enable row level security',v_table);
        execute format('revoke all on movers_private.%I from public,anon,authenticated',v_table);
    end loop;
end $$;

create function movers_private.push_register(p_device uuid,p_environment text,p_token text,p_enabled boolean)
returns void language plpgsql security definer set search_path = '' as $$ begin
    if auth.uid() is null or not exists(select 1 from movers_private.profiles where id=auth.uid()) then
        raise exception 'Sharing identity required' using errcode='42501';
    end if;
    if p_environment not in ('sandbox','production') or p_enabled is null then
        raise exception 'Invalid push registration' using errcode='22023';
    end if;
    if not p_enabled then
        update movers_private.push_devices set enabled=false
            where id=p_device and environment=p_environment and person_id=auth.uid();
        return;
    end if;
    if p_token is null or length(p_token) not between 32 and 512 or p_token !~ '^[0-9a-f]+$' then
        raise exception 'Invalid push token' using errcode='22023';
    end if;
    -- Serialize token reassignment (for example after reinstall) without exposing other registrations.
    perform pg_advisory_xact_lock(hashtextextended(p_environment || p_token,0));
    delete from movers_private.push_devices where environment=p_environment and token=p_token and id<>p_device;
    insert into movers_private.push_devices(id,environment,person_id,token,enabled)
        values(p_device,p_environment,auth.uid(),p_token,true)
        on conflict(id,environment) do update set person_id=excluded.person_id,token=excluded.token,
            enabled=true,registered_at=clock_timestamp();
end $$;

create function movers_private.push_activity() returns trigger
language plpgsql security definer set search_path = '' as $$
declare v_batch uuid; v_recipients uuid[]; begin
    -- Existing command transactions already lock the list before inserting activity.
    if not new.added then
        update movers_private.push_items i set cancelled=true from movers_private.push_batches b
            where b.id=i.batch_id and b.list_id=new.list_id and not b.frozen and i.movie_id=(new.film->>'id')::integer;
        return new;
    end if;
    if not (select enabled from movers_private.push_settings where singleton) then return new; end if;
    select array_agg(person_id) into v_recipients from movers_private.members
        where list_id=new.list_id and active and person_id<>new.actor;
    -- Initial publication has only its author as a member, so creates no notification work.
    if v_recipients is null then return new; end if;
    insert into movers_private.push_batches(list_id,actor,actor_name,due_at)
        values(new.list_id,new.actor,new.actor_snapshot->>'name',clock_timestamp()+interval '5 minutes')
        on conflict(list_id,actor) where not frozen do update
            set due_at=excluded.due_at,actor_name=excluded.actor_name returning id into v_batch;
    insert into movers_private.push_items(event_id,batch_id,movie_id,title,recipients)
        values(new.id,v_batch,(new.film->>'id')::integer,new.film->>'title',v_recipients);
    return new;
end $$;
create trigger push_activity after insert on movers_private.activity
    for each row execute function movers_private.push_activity();

create function movers_private.push_prepare() returns integer
language plpgsql security definer set search_path = '' as $$
declare v_list uuid; v_batch movers_private.push_batches; v_count integer := 0; begin
    if not (select enabled from movers_private.push_settings where singleton) then return 0; end if;
    -- Always take list locks before batch locks, matching command/trigger lock ordering.
    for v_list in select distinct list_id from movers_private.push_batches
        where not frozen and due_at<=now() limit 20 loop
        perform 1 from movers_private.lists where id=v_list for update skip locked;
        if not found then continue; end if;
        for v_batch in select * from movers_private.push_batches
            where list_id=v_list and not frozen and due_at<=now() for update loop
            update movers_private.push_batches set frozen=true where id=v_batch.id;
            insert into movers_private.push_deliveries(batch_id,device_id,environment,person_id,token,content,expires_at)
                select v_batch.id,d.id,d.environment,d.person_id,d.token,
                    jsonb_build_object('listId',l.id,'actor',v_batch.actor_name,'listName',l.name,
                        'count',items.film_count,'filmTitle',items.film_title),v_batch.due_at+interval '24 hours'
                from movers_private.push_devices d
                join movers_private.members m on m.person_id=d.person_id and m.list_id=v_list and m.active
                join movers_private.lists l on l.id=v_list and not l.retired
                cross join lateral (
                    select count(*) as film_count,min(i.title) as film_title from movers_private.push_items i
                    where i.batch_id=v_batch.id and not i.cancelled and d.person_id=any(i.recipients)
                ) items
                where d.enabled and d.person_id<>v_batch.actor and items.film_count>0
                    and v_batch.due_at+interval '24 hours'>now();
            v_count := v_count+1;
        end loop;
    end loop;
    delete from movers_private.push_batches b where b.frozen and b.due_at<now()-interval '30 days';
    return v_count;
end $$;

create function movers_private.push_claim(p_environment text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_result jsonb; begin
    if not (select enabled from movers_private.push_settings where singleton) then return '[]'::jsonb; end if;
    update movers_private.push_deliveries d set state='dropped',lease_id=null,lease_until=null
        where d.environment=p_environment and d.state in ('ready','leased') and (
            d.expires_at<=now() or not exists(select 1 from movers_private.push_devices t
                join movers_private.members m on m.person_id=t.person_id and m.active
                join movers_private.push_batches b on b.id=d.batch_id and b.list_id=m.list_id
                join movers_private.lists l on l.id=b.list_id and not l.retired
                where t.id=d.device_id and t.environment=d.environment and t.enabled
                    and t.person_id=d.person_id and t.token=d.token));
    with candidates as (
        select id from movers_private.push_deliveries
        where environment=p_environment and next_attempt_at<=now()
            and (state='ready' or (state='leased' and lease_until<=now()))
        order by next_attempt_at,id limit 10 for update skip locked
    ), claimed as (
        update movers_private.push_deliveries d set state='leased',attempts=attempts+1,
            lease_id=extensions.gen_random_uuid(),lease_until=now()+interval '2 minutes'
        from candidates c where d.id=c.id
        returning d.id,d.batch_id,d.token,d.content,d.expires_at,d.lease_id,d.attempts
    ) select coalesce(jsonb_agg(jsonb_build_object('id',id,'batchId',batch_id,'token',token,
        'content',content,'expiresAt',extract(epoch from expires_at)::bigint,'leaseId',lease_id,
        'attempts',attempts)),'[]'::jsonb) into v_result from claimed;
    return v_result;
end $$;

create function movers_private.push_finish(p_delivery uuid,p_lease uuid,p_outcome text,
    p_error text,p_retry_seconds integer,p_invalid_before bigint) returns void
language plpgsql security definer set search_path = '' as $$
declare v_delivery movers_private.push_deliveries; begin
    if p_outcome not in ('sent','retry','invalid','failed') then
        raise exception 'Invalid delivery outcome' using errcode='22023';
    end if;
    select * into v_delivery from movers_private.push_deliveries
        where id=p_delivery and state='leased' and lease_id=p_lease for update;
    if not found then return; end if;
    if p_outcome='invalid' then
        update movers_private.push_devices set enabled=false
            where id=v_delivery.device_id and environment=v_delivery.environment and token=v_delivery.token
                and (p_invalid_before is null or registered_at<=to_timestamp(p_invalid_before/1000.0));
    end if;
    update movers_private.push_deliveries set state=case
            when p_outcome='sent' then 'sent'
            when expires_at<=now() or p_outcome='invalid' then 'dropped'
            when p_outcome='retry' then 'ready' else 'failed' end,
        next_attempt_at=now()+make_interval(secs=>greatest(1,least(coalesce(p_retry_seconds,60),3600))),
        last_error=left(p_error,80),lease_id=null,lease_until=null where id=p_delivery;
end $$;

-- Persist provider JWT reuse across short-lived function instances, not just warm process memory.
create function movers_private.push_provider_token(p_environment text,p_key_id text,p_token text)
returns text language plpgsql security definer set search_path = '' as $$
declare v_token text; begin
    perform pg_advisory_xact_lock(hashtextextended('push-provider-' || p_environment,0));
    select token into v_token from movers_private.push_provider_tokens
        where environment=p_environment and key_id=p_key_id and issued_at>now()-interval '30 minutes';
    if v_token is not null then return v_token; end if;
    if p_token is null then return null; end if;
    insert into movers_private.push_provider_tokens(environment,key_id,token,issued_at)
        values(p_environment,p_key_id,p_token,clock_timestamp())
        on conflict(environment) do update set key_id=excluded.key_id,token=excluded.token,issued_at=excluded.issued_at;
    return p_token;
end $$;

create function public.movers_push_register(p_device uuid,p_environment text,p_token text,p_enabled boolean)
returns void language sql security invoker set search_path = '' as $$
    select movers_private.push_register(p_device,p_environment,p_token,p_enabled)
$$;
create function public.movers_push_prepare() returns integer
language sql security invoker set search_path = '' as $$ select movers_private.push_prepare() $$;
create function public.movers_push_claim(p_environment text) returns jsonb
language sql security invoker set search_path = '' as $$ select movers_private.push_claim(p_environment) $$;
create function public.movers_push_finish(p_delivery uuid,p_lease uuid,p_outcome text,
    p_error text,p_retry_seconds integer,p_invalid_before bigint) returns void
language sql security invoker set search_path = '' as $$
    select movers_private.push_finish(p_delivery,p_lease,p_outcome,p_error,p_retry_seconds,p_invalid_before)
$$;
create function public.movers_push_provider_token(p_environment text,p_key_id text,p_token text) returns text
language sql security invoker set search_path = '' as $$
    select movers_private.push_provider_token(p_environment,p_key_id,p_token)
$$;
revoke all on function movers_private.push_activity() from public,anon,authenticated;
revoke all on function movers_private.push_register(uuid,text,text,boolean),
    public.movers_push_register(uuid,text,text,boolean) from public,anon;
grant execute on function movers_private.push_register(uuid,text,text,boolean),
    public.movers_push_register(uuid,text,text,boolean) to authenticated;
grant usage on schema movers_private to service_role;
do $$ declare v_function record; begin
    for v_function in select p.oid::regprocedure as signature from pg_proc p
        join pg_namespace n on n.oid=p.pronamespace
        where n.nspname in ('public','movers_private') and p.proname in (
            'push_prepare','push_claim','push_finish','push_provider_token',
            'movers_push_prepare','movers_push_claim','movers_push_finish','movers_push_provider_token') loop
        execute format('revoke all on function %s from public,anon,authenticated',v_function.signature);
        execute format('grant execute on function %s to service_role',v_function.signature);
    end loop;
end $$;
