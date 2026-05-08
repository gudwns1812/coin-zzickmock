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

test("login route redirects authenticated users before rendering the client form", () => {
  const loginPageSource = readFrontendSource("app/login/page.tsx");
  const loginFormSource = readFrontendSource("app/login/LoginFormClient.tsx");

  assert.doesNotMatch(loginPageSource, /"use client"/);
  assert.match(loginPageSource, /const authUser = await getAuthUser\(\);/);
  assert.match(loginPageSource, /if \(authUser\) \{/);
  assert.match(loginPageSource, /redirect\("\/markets"\);/);
  assert.match(loginPageSource, /return <LoginFormClient \/>;/);
  assert.match(loginFormSource, /"use client"/);
});

test("frontend authenticated-user lookup uses the non-mutating auth me endpoint", () => {
  const futuresApiSource = readFrontendSource("lib/futures-api.ts");

  assert.match(futuresApiSource, /readApiResult<AuthUser>\("\/api\/futures\/auth\/me"\)/);
  assert.doesNotMatch(
    futuresApiSource,
    /readApiResult<AuthUser>\("\/api\/futures\/auth\/refresh"\)/
  );
});
