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

test("header login control starts Google login without password inputs", () => {
  const loginFormSource = readFrontendSource(
    "components/ui/shared/header/LoginForm.tsx"
  );

  assert.match(loginFormSource, /createGoogleLoginUrl/);
  assert.match(loginFormSource, /Google 로그인/);
  assert.doesNotMatch(loginFormSource, /password/);
  assert.doesNotMatch(loginFormSource, /loginToFutures/);
});

test("header logout form uses explicit client submit handling", () => {
  const logoutFormSource = readFrontendSource(
    "components/ui/shared/header/LogoutForm.tsx"
  );

  assert.doesNotMatch(logoutFormSource, /useFormStatus/);
  assert.match(logoutFormSource, /event\.preventDefault\(\)/);
  assert.match(logoutFormSource, /logoutFromFutures\(\)/);
  assert.match(logoutFormSource, /notifyFuturesAuthChanged\("logout"\)/);
  assert.match(logoutFormSource, /router\.refresh\(\)/);
  assert.match(logoutFormSource, /<form onSubmit=\{handleLogout\}>/);
});
