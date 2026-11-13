-- Run only after configuring Vault and validating the deployed function. No app build runs this file.
-- Required Vault secret names: movers_push_project_url, movers_push_dispatch_secret.
create extension if not exists pg_cron;
create extension if not exists pg_net with schema extensions;
select cron.schedule('movers-watchlist-notifications','* * * * *',$job$
    select net.http_post(
        url := (select decrypted_secret from vault.decrypted_secrets where name='movers_push_project_url')
            || '/functions/v1/watchlist-notifications',
        headers := jsonb_build_object('Content-Type','application/json','Authorization','Bearer ' ||
            (select decrypted_secret from vault.decrypted_secrets where name='movers_push_dispatch_secret')),
        body := '{}'::jsonb,
        timeout_milliseconds := 120000
    );
$job$);
