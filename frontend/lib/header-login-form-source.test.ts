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

test("header login form submits from Enter in login inputs", () => {
  const loginFormSource = readFrontendSource(
    "components/ui/shared/header/LoginForm.tsx"
  );

  assert.match(loginFormSource, /handleLoginInputKeyDown/);
  assert.match(loginFormSource, /e\.key !== "Enter"/);
  assert.match(loginFormSource, /e\.target instanceof HTMLInputElement/);
  assert.match(loginFormSource, /e\.currentTarget\.requestSubmit\(\)/);
  assert.match(loginFormSource, /onKeyDown=\{handleLoginInputKeyDown\}/);
});
