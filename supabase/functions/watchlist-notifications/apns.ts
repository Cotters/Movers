export type Environment = "sandbox" | "production";

export interface Delivery {
  id: string;
  batchId: string;
  token: string;
  leaseId: string;
  expiresAt: number;
  attempts: number;
  content: {
    listId: string;
    actor: string;
    listName: string;
    count: number;
    filmTitle: string;
  };
}

export interface Outcome {
  outcome: "sent" | "retry" | "invalid" | "failed";
  error: string | null;
  retrySeconds: number;
  invalidBefore: number | null;
  configurationError: boolean;
}

const encoder = new TextEncoder();
const segmenter = new Intl.Segmenter("en", { granularity: "grapheme" });

function shorten(value: string, maximum: number): string {
  const characters = Array.from(segmenter.segment(value.replace(/\s+/gu, " ").trim()), (part) => part.segment);
  return characters.length > maximum ? characters.slice(0, maximum).join("") + "…" : characters.join("");
}

export function payload(delivery: Delivery): string {
  const content = delivery.content;
  const actor = shorten(content.actor, 40);
  const list = shorten(content.listName, 10);
  const films = content.count === 1 ? shorten(content.filmTitle, 100) : `${content.count} films`;
  const result = JSON.stringify({
    aps: {
      alert: { title: "Movers", body: `${actor} added ${films} to ${list}` },
      sound: "default",
      "thread-id": `watchlist-${content.listId}`,
      "interruption-level": "active",
    },
    version: 1,
    type: "watchlist_additions",
    groupId: delivery.batchId,
    listId: content.listId,
  });
  if (encoder.encode(result).length > 4096) throw new Error("PayloadTooLarge");
  return result;
}

function base64url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/u, "");
}

export async function providerToken(teamId: string, keyId: string, pem: string): Promise<string> {
  const encodedKey = pem.replace(/-----[^-]+-----|\s/gu, "");
  const key = await crypto.subtle.importKey(
    "pkcs8",
    Uint8Array.from(atob(encodedKey), (value) => value.charCodeAt(0)),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  const header = base64url(encoder.encode(JSON.stringify({ alg: "ES256", kid: keyId })));
  const claims = base64url(encoder.encode(JSON.stringify({ iss: teamId, iat: Math.floor(Date.now() / 1000) })));
  const message = `${header}.${claims}`;
  const signature = await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, encoder.encode(message));
  return `${message}.${base64url(new Uint8Array(signature))}`;
}

export async function send(delivery: Delivery, environment: Environment, jwt: string): Promise<Outcome> {
  const started = Date.now();
  const retrySeconds = Math.min(3600, 60 * 2 ** Math.min(delivery.attempts - 1, 6)) + Math.floor(Math.random() * 30);
  const outcome: Outcome = {
    outcome: "retry",
    error: null,
    retrySeconds,
    invalidBefore: null,
    configurationError: false,
  };
  try {
    const host = environment === "sandbox" ? "api.sandbox.push.apple.com" : "api.push.apple.com";
    const response = await fetch(`https://${host}/3/device/${delivery.token}`, {
      method: "POST",
      headers: {
        authorization: `bearer ${jwt}`,
        "content-type": "application/json",
        "apns-topic": "com.jcotters.movers",
        "apns-push-type": "alert",
        "apns-priority": "5",
        "apns-id": delivery.id,
        "apns-collapse-id": delivery.id,
        "apns-expiration": String(delivery.expiresAt),
      },
      body: payload(delivery),
      signal: AbortSignal.timeout(10_000),
    });
    if (response.ok) return { ...outcome, outcome: "sent" };
    const body = await response.json().catch(() => ({})) as { reason?: string; timestamp?: number };
    outcome.error = body.reason ?? `HTTP${response.status}`;
    if (response.status === 410 || body.reason === "BadDeviceToken") {
      outcome.outcome = "invalid";
      outcome.invalidBefore = body.timestamp ?? started;
    } else if (response.status === 403 || body.reason === "DeviceTokenNotForTopic" || body.reason === "BadTopic") {
      outcome.configurationError = true;
      outcome.retrySeconds = 1800;
    } else if (response.status !== 429 && response.status < 500) {
      outcome.outcome = "failed";
    }
    const retryAfter = Number(response.headers.get("retry-after"));
    if (Number.isFinite(retryAfter) && retryAfter > 0) {
      outcome.retrySeconds = Math.max(outcome.retrySeconds, retryAfter);
    }
    return outcome;
  } catch (error) {
    // Only fixed categories cross the logging/persistence boundary; never raw network errors or payloads.
    outcome.error = error instanceof Error && error.message === "PayloadTooLarge"
      ? "PayloadTooLarge"
      : "TransportError";
    if (outcome.error === "PayloadTooLarge") outcome.outcome = "failed";
    return outcome;
  }
}
