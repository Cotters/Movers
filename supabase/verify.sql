-- Deployment checks only; does not create users or feature data.
do $$
declare
    v_name text;
    v_signature text;
    v_function oid;
begin
    foreach v_name in array array['profiles','lists','members','entries','activity','invites','receipts',
        'push_settings','push_devices','push_batches','push_items','push_deliveries','push_provider_tokens'] loop
        if not exists(select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
            where n.nspname='movers_private' and c.relname=v_name and c.relkind='r' and c.relrowsecurity) then
            raise exception 'Missing private table or RLS: %', v_name;
        end if;
    end loop;
    if not exists(select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
        where n.nspname='public' and c.relname='movers_list_signals' and c.relrowsecurity) then
        raise exception 'Missing list signals or RLS';
    end if;
    if not exists(select 1 from pg_policies where schemaname='public' and tablename='movers_list_signals'
        and policyname='signals_member' and cmd='SELECT' and 'authenticated'=any(roles)) then
        raise exception 'Missing member-only Realtime policy';
    end if;
    if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname like 'movers_%' and p.prosecdef) then
        raise exception 'Unsafe exposed definer RPC';
    end if;
    foreach v_signature in array array[
        'public.movers_profile(text,integer)', 'public.movers_snapshot(uuid)',
        'public.movers_preview(text)', 'public.movers_command(uuid,jsonb)', 'public.movers_import(uuid,jsonb)',
        'public.movers_invite(uuid)', 'public.movers_activity(uuid,bigint)', 'public.movers_list_ids()',
        'public.movers_push_register(uuid,text,text,boolean)'
    ] loop
        v_function := to_regprocedure(v_signature);
        if v_function is null then
            raise exception 'Missing RPC: %', v_signature;
        end if;
        if not has_function_privilege('authenticated',v_function,'execute') then
            raise exception 'Authenticated role cannot call RPC: %', v_signature;
        end if;
    end loop;
    foreach v_signature in array array['public.movers_push_prepare()', 'public.movers_push_claim(text)',
        'public.movers_push_finish(uuid,uuid,text,text,integer,bigint)',
        'public.movers_push_provider_token(text,text,text)'] loop
        v_function := to_regprocedure(v_signature);
        if v_function is null or not has_function_privilege('service_role',v_function,'execute')
            or has_function_privilege('authenticated',v_function,'execute') then
            raise exception 'Missing or exposed notification worker RPC: %',v_signature;
        end if;
    end loop;
    if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname like 'movers_%' and has_function_privilege('anon',p.oid,'execute')) then
        raise exception 'Unauthenticated role can call a protected RPC';
    end if;
    if exists(select 1 from information_schema.role_table_grants where table_schema='movers_private'
        and grantee in ('anon','authenticated','PUBLIC')) then
        raise exception 'Direct private-table grants exposed to clients';
    end if;
    if not exists(select 1 from pg_publication_tables where pubname='supabase_realtime'
        and schemaname='public' and tablename='movers_list_signals') then
        raise exception 'List signals are not published to Realtime';
    end if;
end $$;
select 'Schema/RLS/RPC/publication deployment checks passed' as result;
