import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const sourceRoot = new URL("..", import.meta.url).pathname;

function read(path: string): string {
  return readFileSync(join(sourceRoot, path), "utf8");
}

test("leaderboard position peek row selection toggles the same target closed", () => {
  const landingSource = read("components/router/(main)/markets/MarketsLanding.tsx");

  assert.equal(
    landingSource.includes("currentTarget?.targetToken === target.targetToken ? null : target"),
    true
  );
  assert.equal(
    landingSource.includes("onSelect={entry.targetToken ? togglePeekTarget : undefined}"),
    true
  );
});
