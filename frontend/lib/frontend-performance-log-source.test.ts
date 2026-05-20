import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function readFrontendSource(relativePath: string) {
  return readFileSync(path.join(__dirname, "..", relativePath), "utf8");
}

test("frontend API wrapper joins browser and backend request logs", () => {
  const wrapperSource = readFrontendSource("lib/frontend-performance-log.ts");
  const apiRequestSource = readFrontendSource("lib/futures-api-request.ts");

  assert.match(wrapperSource, /"frontend\.api\.completed"/);
  assert.match(wrapperSource, /X-Request-Id/);
  assert.match(wrapperSource, /X-Correlation-Id/);
  assert.match(apiRequestSource, /fetchWithFrontendTiming/);
});

test("frontend API path patterns mask dynamic identifiers", () => {
  const apiRequestSource = readFrontendSource("lib/futures-api-request.ts");

  assert.match(apiRequestSource, /\{orderId\}/);
  assert.match(apiRequestSource, /\{postId\}/);
  assert.match(apiRequestSource, /\{requestId\}/);
  assert.match(apiRequestSource, /replaceAll\(\/\\\/\\d\+/);
});

test("root layout installs page performance logging", () => {
  const layoutSource = readFrontendSource("app/layout.tsx");

  assert.match(layoutSource, /FrontendPerformanceLogger/);
  assert.match(
    readFrontendSource("components/router/FrontendPerformanceLogger.tsx"),
    /logFrontendPageTiming/
  );
});
