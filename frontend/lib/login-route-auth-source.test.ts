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

test("login route leaves auth ownership with the backend", () => {
  const loginPageSource = readFrontendSource("app/login/page.tsx");
  const loginFormSource = readFrontendSource("app/login/LoginFormClient.tsx");

  assert.doesNotMatch(loginPageSource, /"use client"/);
  assert.doesNotMatch(loginPageSource, /getAuthUser/);
  assert.doesNotMatch(loginPageSource, /redirect\("\/markets"\);/);
  assert.match(loginPageSource, /return <LoginFormClient \/>;/);
  assert.match(loginFormSource, /"use client"/);
  assert.match(loginFormSource, /loginToFutures/);
  assert.match(loginFormSource, /notifyFuturesAuthChanged/);
});

test("frontend authenticated-user lookup uses the non-mutating auth me endpoint", () => {
  const authStateSource = readFrontendSource("lib/futures-auth-state.ts");
  const futuresApiSource = readFrontendSource("lib/futures-api.ts");

  assert.match(authStateSource, /fetchFuturesBackendApi\("\/auth\/me"/);
  assert.match(authStateSource, /credentials: "include"/);
  assert.doesNotMatch(
    authStateSource,
    /fetchFuturesBackendApi\("\/auth\/refresh"/
  );
  assert.doesNotMatch(futuresApiSource, /getAccessTokenCookieHeader/);
});

test("server-rendered futures public data does not forward request cookies to the backend", () => {
  const futuresApiSource = readFrontendSource("lib/futures-api.ts");

  assert.doesNotMatch(futuresApiSource, /from "next\/headers"/);
  assert.doesNotMatch(futuresApiSource, /await cookies\(\)/);
  assert.doesNotMatch(futuresApiSource, /Cookie: cookieHeader/);
  assert.doesNotMatch(futuresApiSource, /getBackendCookieHeader\(\)/);
});

test("authenticated screens re-check backend auth after login/logout events", () => {
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const marketDetailSource = readFrontendSource(
    "components/futures/MarketDetailRealtimeView.tsx"
  );
  const marketsLandingSource = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  for (const source of [authGateSource, marketDetailSource, marketsLandingSource]) {
    assert.match(source, /FUTURES_AUTH_CHANGED_EVENT/);
    assert.match(source, /window\.addEventListener\(FUTURES_AUTH_CHANGED_EVENT/);
    assert.match(source, /window\.removeEventListener\(FUTURES_AUTH_CHANGED_EVENT/);
    assert.match(source, /getFuturesAuthUserClient\(\)/);
  }

  assert.match(marketsLandingSource, /effectiveIsAuthenticated/);
  assert.match(marketsLandingSource, /isAuthenticated=\{effectiveIsAuthenticated\}/);
  assert.doesNotMatch(marketsLandingSource, /isAuthenticated=\{isAuthenticated\}/);
});
