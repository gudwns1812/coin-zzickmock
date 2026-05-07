export const SSE_CLIENT_KEY_PARAM = "clientKey";

const TAB_CLIENT_KEY_STORAGE_KEY = "coin-zzickmock:sse-tab-client-key";
const CLIENT_KEY_PREFIX = "tab:";

type ClientKeyStorage = Pick<Storage, "getItem" | "setItem">;

function createRandomId() {
  const cryptoApi = globalThis.crypto;

  if (cryptoApi && typeof cryptoApi.randomUUID === "function") {
    return cryptoApi.randomUUID();
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

function createTabClientKey() {
  return `${CLIENT_KEY_PREFIX}${createRandomId()}`;
}

function getSessionStorage(): ClientKeyStorage | null {
  if (typeof window === "undefined") {
    return null;
  }

  return window.sessionStorage;
}

export function getOrCreateTabSseClientKey(
  storage: ClientKeyStorage | null = getSessionStorage()
) {
  if (!storage) {
    return createTabClientKey();
  }

  try {
    const current = storage.getItem(TAB_CLIENT_KEY_STORAGE_KEY);
    if (current && current.trim()) {
      return current;
    }

    const next = createTabClientKey();
    storage.setItem(TAB_CLIENT_KEY_STORAGE_KEY, next);
    return next;
  } catch {
    return createTabClientKey();
  }
}

export function normalizeSseClientKey(clientKey: string | null | undefined) {
  const normalized = clientKey?.trim();
  return normalized ? normalized : null;
}

export function appendSseClientKey(url: string, clientKey: string | null | undefined) {
  const normalizedClientKey = normalizeSseClientKey(clientKey);
  if (!normalizedClientKey) {
    return null;
  }

  const parsedUrl = new URL(url, "http://coin-zzickmock.local");
  parsedUrl.searchParams.set(SSE_CLIENT_KEY_PARAM, normalizedClientKey);

  if (/^https?:\/\//.test(url)) {
    return parsedUrl.toString();
  }

  return `${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`;
}

export function readRequiredSseClientKey(url: string) {
  return normalizeSseClientKey(new URL(url).searchParams.get(SSE_CLIENT_KEY_PARAM));
}
