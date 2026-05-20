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
  assert.match(loginFormSource, /notifyFuturesAuthChanged\("login"\)/);
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

test("authenticated screens share the react-query auth cache", () => {
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const marketDetailSource = readFrontendSource(
    "components/futures/MarketDetailRealtimeView.tsx"
  );
  const marketsLandingSource = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  for (const source of [authGateSource, marketDetailSource, marketsLandingSource]) {
    assert.match(source, /useFuturesAuthUser/);
    assert.doesNotMatch(source, /window\.addEventListener\(FUTURES_AUTH_CHANGED_EVENT/);
    assert.doesNotMatch(source, /window\.removeEventListener\(FUTURES_AUTH_CHANGED_EVENT/);
    assert.doesNotMatch(source, /getFuturesAuthUserClient\(\)/);
  }

  assert.match(marketsLandingSource, /effectiveIsAuthenticated/);
  assert.match(marketsLandingSource, /isAuthenticated=\{effectiveIsAuthenticated\}/);
  assert.match(marketsLandingSource, /enabled: Boolean\(authUser\)/);
  assert.doesNotMatch(marketsLandingSource, /isAuthenticated=\{_isAuthenticated\}/);
});

test("auth events encode explicit cache-policy actions", () => {
  const authStateSource = readFrontendSource("lib/futures-auth-state.ts");
  const loginPageSource = readFrontendSource("app/login/LoginFormClient.tsx");
  const headerLoginSource = readFrontendSource("components/ui/shared/header/LoginForm.tsx");
  const logoutSource = readFrontendSource("components/ui/shared/header/LogoutForm.tsx");
  const withdrawalSource = readFrontendSource("components/ui/shared/header/WithdrawalForm.tsx");

  assert.match(authStateSource, /type FuturesAuthChangeAction/);
  assert.match(authStateSource, /new CustomEvent\(FUTURES_AUTH_CHANGED_EVENT/);
  assert.match(loginPageSource, /notifyFuturesAuthChanged\("login"\)/);
  assert.match(headerLoginSource, /notifyFuturesAuthChanged\("login"\)/);
  assert.match(logoutSource, /notifyFuturesAuthChanged\("logout"\)/);
  assert.match(withdrawalSource, /notifyFuturesAuthChanged\("withdraw"\)/);
});

test("logout does not probe auth me after logout succeeds", () => {
  const authClientSource = readFrontendSource("lib/futures-auth-client.ts");
  const logoutFunctionSource = authClientSource.slice(
    authClientSource.indexOf("export async function logoutFromFutures"),
    authClientSource.length
  );

  assert.match(logoutFunctionSource, /fetchFuturesBackendApi\("\/auth\/logout"/);
  assert.match(logoutFunctionSource, /return response\.ok/);
  assert.doesNotMatch(logoutFunctionSource, /\/auth\/me/);
});
