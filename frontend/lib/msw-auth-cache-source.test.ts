import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.join(__dirname, "..");

function readFrontendSource(relativePath: string): string {
  return readFileSync(path.join(rootDir, relativePath), "utf8");
}

test("MSWProvider renders children without blocking app mount", () => {
  const providerSource = readFrontendSource("app/MSWProvider.tsx");

  assert.match(providerSource, /void ensureMswReady\(\)/);
  assert.match(providerSource, /return <>\{children\}<\/>/);
  assert.doesNotMatch(providerSource, /return null/);
  assert.doesNotMatch(providerSource, /useState/);
});

test("MSW readiness is idempotent and dynamically imports the browser worker", () => {
  const mswReadySource = readFrontendSource("lib/msw-ready.ts");

  assert.match(mswReadySource, /let mswReadyPromise: Promise<void> \| null = null/);
  assert.match(mswReadySource, /if \(!mswReadyPromise\)/);
  assert.match(mswReadySource, /mswReadyPromise = import\("@\/mocks\/browser"\)/);
  assert.match(mswReadySource, /worker\.start\(\{/);
  assert.match(mswReadySource, /onUnhandledRequest: "bypass"/);
  assert.match(mswReadySource, /console\.warn/);
  assert.doesNotMatch(mswReadySource, /from "@\/mocks\/browser"/);
});

test("futures backend fetches await MSW readiness at the request boundary", () => {
  const requestSource = readFrontendSource("lib/futures-api-request.ts");

  assert.match(requestSource, /import \{ ensureMswReady \} from "@\/lib\/msw-ready"/);
  assert.match(requestSource, /export async function fetchFuturesBackendApi/);
  assert.match(requestSource, /await ensureMswReady\(\)/);
  assert.match(requestSource, /fetchWithFrontendTiming/);
});
