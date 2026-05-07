import assert from "node:assert/strict";
import test from "node:test";

const moduleUrl = new URL("./sse-client-key.ts", import.meta.url).href;
const {
  appendSseClientKey,
  getOrCreateTabSseClientKey,
  normalizeSseClientKey,
  readRequiredSseClientKey,
}: typeof import("./sse-client-key") = await import(moduleUrl);

function createStorage(initialValue?: string) {
  const values = new Map<string, string>();
  if (initialValue) {
    values.set("coin-zzickmock:sse-tab-client-key", initialValue);
  }

  return {
    getItem(key: string) {
      return values.get(key) ?? null;
    },
    setItem(key: string, value: string) {
      values.set(key, value);
    },
    value() {
      return values.get("coin-zzickmock:sse-tab-client-key");
    },
  };
}

test("tab SSE client key reuses a sessionStorage value", () => {
  const storage = createStorage("tab:existing");

  assert.equal(getOrCreateTabSseClientKey(storage), "tab:existing");
});

test("tab SSE client key creates and stores a tab-scoped value", () => {
  const storage = createStorage();
  const clientKey = getOrCreateTabSseClientKey(storage);

  assert.equal(clientKey.startsWith("tab:"), true);
  assert.equal(storage.value(), clientKey);
});

test("tab SSE client key falls back to a stable in-memory value when storage is unavailable", () => {
  const firstClientKey = getOrCreateTabSseClientKey(null);
  const secondClientKey = getOrCreateTabSseClientKey(null);

  assert.equal(firstClientKey.startsWith("tab:"), true);
  assert.equal(secondClientKey, firstClientKey);
});

test("tab SSE client key falls back to memory when sessionStorage access throws", () => {
  const storage = {
    getItem() {
      throw new Error("storage unavailable");
    },
    setItem() {
      throw new Error("storage unavailable");
    },
  };

  const clientKey = getOrCreateTabSseClientKey(storage);

  assert.equal(clientKey.startsWith("tab:"), true);
});

test("appendSseClientKey preserves existing params and encodes clientKey", () => {
  const url = appendSseClientKey(
    "/api/futures/markets/BTCUSDT/candles/stream?interval=1m",
    "tab:client key"
  );

  assert.equal(
    url,
    "/api/futures/markets/BTCUSDT/candles/stream?interval=1m&clientKey=tab%3Aclient+key"
  );
});

test("appendSseClientKey returns null for missing clientKey", () => {
  assert.equal(appendSseClientKey("/api/futures/orders/stream", " "), null);
});

test("readRequiredSseClientKey trims and requires the query param", () => {
  assert.equal(
    readRequiredSseClientKey("http://localhost/api/futures/orders/stream?clientKey=%20tab-1%20"),
    "tab-1"
  );
  assert.equal(
    readRequiredSseClientKey("http://localhost/api/futures/orders/stream"),
    null
  );
});

test("normalizeSseClientKey treats blank values as missing", () => {
  assert.equal(normalizeSseClientKey("  "), null);
  assert.equal(normalizeSseClientKey(" tab-1 "), "tab-1");
});


test("tab SSE client key utility does not use localStorage", async () => {
  const { readFileSync } = await import("node:fs");
  const { fileURLToPath } = await import("node:url");
  const source = readFileSync(fileURLToPath(moduleUrl), "utf8");

  assert.equal(source.includes("localStorage"), false);
});
