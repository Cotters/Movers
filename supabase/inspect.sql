-- Metadata only: no user records, sessions or invitations.
select n.nspname as schema, c.relname as table_name, c.relrowsecurity as rls_enabled
from pg_class c join pg_namespace n on n.oid=c.relnamespace
where c.relkind in ('r','p') and n.nspname in ('public','movers_private')
order by n.nspname,c.relname;
