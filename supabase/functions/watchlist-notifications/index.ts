import { type Delivery, type Environment, providerToken, send } from "./apns.ts";

function required(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error("MissingServerConfiguration");
  return value;
}

async function authorized(request: Request): Promise<boolean> {
  const expected = Deno.env.get("PUSH_DISPATCH_SECRET");
  if (!expected) return false;
  const supplied = request.headers.get("authorization")?.replace(/^Bearer /u, "") ?? "";
  const digest = async (value: string) =>
    new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
  const [first, second] = await Promise.all([digest(expected), digest(supplied)]);
  let difference = 0;
  for (let index = 0; index < first.length; index++) difference |= first[index] ^ second[index];
  return difference === 0;
}

async function rpc<T>(name: string, parameters: Record<string, unknown> = {}): Promise<T> {
  const key = required("SUPABASE_SERVICE_ROLE_KEY");
  const response = await fetch(`${required("SUPABASE_URL")}/rest/v1/rpc/${name}`, {
    method: "POST",
    headers: { apikey: key, authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify(parameters),
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    await response.body?.cancel();
    throw new Error("NotificationStorageUnavailable");
  }
  const body = await response.text();
  return (body ? JSON.parse(body) : null) as T;
}

async function authentication(environment: Environment): Promise<string> {
  const prefix = `APNS_${environment.toUpperCase()}`;
  const keyId = required(`${prefix}_KEY_ID`);
  const teamId = required("APNS_TEAM_ID");
  const parameters = { p_environment: environment, p_key_id: keyId };
  const existing = await rpc<string | null>("movers_push_provider_token", { ...parameters, p_token: null });
  if (existing) return existing;
  const token = await providerToken(teamId, keyId, required(`${prefix}_PRIVATE_KEY`));
  return await rpc<string>("movers_push_provider_token", { ...parameters, p_token: token });
}

async function dispatch(environment: Environment, jwt: string): Promise<boolean> {
  const deliveries = await rpc<Delivery[]>("movers_push_claim", { p_environment: environment });
  let configurationError = false;
  // Bounded concurrency remains within hosted function limits and avoids a burst of APNs requests.
  for (let offset = 0; offset < deliveries.length; offset += 5) {
    if (configurationError) break; // Unstarted leases expire and become eligible for the next healthy worker.
    const results = await Promise.all(
      deliveries.slice(offset, offset + 5).map(async (delivery) => {
        const result = await send(delivery, environment, jwt);
        await rpc("movers_push_finish", {
          p_delivery: delivery.id,
          p_lease: delivery.leaseId,
          p_outcome: result.outcome,
          p_error: result.error,
          p_retry_seconds: result.retrySeconds,
          p_invalid_before: result.invalidBefore,
        });
        console.log(JSON.stringify({ deliveryId: delivery.id, outcome: result.outcome, reason: result.error }));
        return result.configurationError;
      }),
    );
    configurationError = results.some(Boolean);
  }
  return configurationError;
}

Deno.serve(async (request: Request): Promise<Response> => {
  if (request.method !== "POST") return new Response(null, { status: 405 });
  if (!await authorized(request)) return new Response(null, { status: 401 });
  if (Deno.env.get("PUSH_ENABLED") !== "true") return Response.json({ enabled: false });
  try {
    const environments = required("APNS_ENVIRONMENTS").split(",").map((value) => value.trim());
    if (environments.length === 0 || environments.some((value) => value !== "sandbox" && value !== "production")) {
      throw new Error("InvalidEnvironmentConfiguration");
    }
    // Validate every enabled environment before claiming work; no secret means no attempt is consumed.
    const tokens = new Map<Environment, string>();
    for (const environment of new Set(environments as Environment[])) {
      tokens.set(environment, await authentication(environment));
    }
    const prepared = await rpc<number>("movers_push_prepare");
    let configurationError = false;
    for (const [environment, jwt] of tokens) {
      configurationError = await dispatch(environment, jwt) || configurationError;
    }
    return Response.json({ prepared, configurationError }, { status: configurationError ? 503 : 200 });
  } catch {
    console.error("Notification dispatcher failed; inspect configuration, RPC availability and overdue deliveries.");
    return Response.json({ error: "NotificationDispatcherUnavailable" }, { status: 503 });
  }
});
