-- Preserve existing command signatures/receipts; add outcomes only for import batches.
alter table movers_private.receipts add column result jsonb;

create or replace function movers_private.command(p_request uuid, p_command jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_actor uuid := auth.uid(); v_list uuid := (p_command->>'listId')::uuid;
    v_kind text := split_part(p_command->>'type','.',-1);
    v_film jsonb; v_entry movers_private.entries; v_id integer; v_changed boolean;
    v_hash bytea := extensions.digest(p_command::text,'sha256'); v_existing bytea;
    v_at bigint := movers_private.now_ms(); v_preview jsonb;
    v_outcomes jsonb := '[]'::jsonb; v_result jsonb; v_disposition text;
    v_any_imported boolean := false;
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
            when 'ImportFilms' then
                if jsonb_typeof(p_command->'films') is distinct from 'array'
                    or jsonb_array_length(p_command->'films') not between 1 and 50 then
                    raise exception 'Import up to 50 films per batch' using errcode='22023';
                end if;
                for v_film in select value from jsonb_array_elements(p_command->'films') loop
                    v_id := null;
                    begin
                        v_id := (v_film->>'id')::integer;
                        if not movers_private.valid_film(v_film) then
                            v_disposition := 'Rejected';
                        else
                            insert into movers_private.entries values(v_list,v_id,v_film,v_actor,v_at,true)
                                on conflict(list_id,movie_id) do nothing;
                            if found then
                                v_disposition := 'Imported'; v_any_imported := true;
                                insert into movers_private.activity(list_id,actor,actor_snapshot,film,added,at,imported)
                                    values(v_list,v_actor,movers_private.person(v_actor),v_film,true,v_at,true);
                            else v_disposition := 'AlreadyPresent'; end if;
                        end if;
                    exception when invalid_text_representation or numeric_value_out_of_range or check_violation or not_null_violation then
                        v_disposition := 'Rejected';
                    end;
                    v_outcomes := v_outcomes || jsonb_build_array(jsonb_build_object(
                        'movieId',coalesce(v_id,0),'disposition',v_disposition));
                end loop;
                if v_any_imported then update movers_private.lists set updated_at=v_at where id=v_list; end if;
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
    if v_kind <> 'ImportFilms' or v_any_imported then perform movers_private.touch(v_list); end if;
    if v_kind = 'ImportFilms' then
        select jsonb_build_object('outcomes',v_outcomes,'revision',revision) into v_result
            from movers_private.lists where id=v_list;
    end if;
    insert into movers_private.receipts(person_id,request_id,command_hash,result) values(v_actor,p_request,v_hash,v_result);
end $$;

create function movers_private.import_films(p_request uuid, p_command jsonb) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_result jsonb; begin
    if split_part(p_command->>'type','.',-1) is distinct from 'ImportFilms' then
        raise exception 'Invalid import command' using errcode='22023';
    end if;
    if not movers_private.member((p_command->>'listId')::uuid) then
        raise exception 'Access denied' using errcode='42501';
    end if;
    perform movers_private.command(p_request,p_command);
    select result into v_result from movers_private.receipts where person_id=auth.uid() and request_id=p_request;
    return v_result;
end $$;

create function public.movers_import(p_request uuid, p_command jsonb) returns jsonb
language sql security invoker set search_path = '' as $$ select movers_private.import_films(p_request,p_command) $$;
revoke all on function movers_private.import_films(uuid,jsonb) from public, anon;
revoke all on function public.movers_import(uuid,jsonb) from public, anon;
grant execute on function movers_private.import_films(uuid,jsonb) to authenticated;
grant execute on function public.movers_import(uuid,jsonb) to authenticated;
