import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const sourceRoot = new URL("..", import.meta.url).pathname;

function read(path: string): string {
  return readFileSync(join(sourceRoot, path), "utf8");
}

test("position peek consume uses the created snapshot response immediately", () => {
  const clientSource = read("lib/futures-client-api.ts");
  const landingSource = read("components/router/(main)/markets/MarketsLanding.tsx");

  assert.equal(clientSource.includes("): Promise<PositionPeekSnapshot>"), true);
  assert.equal(
    clientSource.includes("return writeFuturesApi<PositionPeekSnapshot>(\"/position-peeks\""),
    true
  );
  assert.equal(
    landingSource.includes("const snapshot = status?.latestSnapshot ?? null;"),
    true
  );
  assert.equal(
    landingSource.includes("latestSnapshot: snapshot"),
    true
  );
});

test("position peek panel renders only the selected target cache", () => {
  const landingSource = read("components/router/(main)/markets/MarketsLanding.tsx");

  assert.equal(
    landingSource.includes("const snapshot = consumeMutation.data ??"),
    false
  );
  assert.equal(
    landingSource.includes("consumeMutation.data?.remainingPeekItemCount"),
    false
  );
});
