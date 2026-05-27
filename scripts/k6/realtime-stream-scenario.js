import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
  eventStreamHeaders,
  precreatedUserCount,
  precreatedUserForVu,
  setupPrecreatedUsers,
  supportedSymbolsFromEnv,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

const BASE_URL = (__ENV.BASE_URL || "http://host.docker.internal:18080").replace(/\/$/, "");
const SYMBOLS = supportedSymbolsFromEnv();
const STREAM_TIMEOUT = __ENV.STREAM_TIMEOUT || "60s"; // SSE connection holding duration
let orderStreamAccessToken = null;

// Custom metrics to observe persistent connections
const activeConnections = new Counter("active_stream_connections");
const connectionDurations = new Trend("stream_connection_duration_ms");
const connectionFailures = new Counter("stream_connection_failures");

export const options = {
  scenarios: {
    // 1. Market Summary stream subscription (Anonymous, 400 VUs)
    market_summary_stream: {
      executor: "constant-vus",
      vus: 400,
      duration: "5m",
      exec: "marketSummaryStreamFlow",
    },
    // 2. Market Candle stream subscription (Anonymous, 300 VUs)
    market_candle_stream: {
      executor: "constant-vus",
      vus: 300,
      duration: "5m",
      exec: "marketCandleStreamFlow",
    },
    // 3. User Order/Position update execution stream (Authenticated, 300 VUs)
    order_execution_stream: {
      executor: "constant-vus",
      vus: 300,
      duration: "5m",
      exec: "orderExecutionStreamFlow",
    },
  },
  thresholds: {
    stream_connection_failures: ["count<50"], // Expect extremely low network/server connect errors
  },
  noCookiesReset: true,
};

export function setup() {
  return setupPrecreatedUsers(BASE_URL, {
    prefix: "k6_sse",
    count: precreatedUserCount(300),
  });
}

// Helper: Pick a random symbol from list
function randomSymbol() {
  return SYMBOLS[Math.floor(Math.random() * SYMBOLS.length)].trim();
}

// ----------------------------------------------------
// A. Market Summary Stream Flow (Anonymous)
// ----------------------------------------------------
export function marketSummaryStreamFlow() {
  const symbolsQuery = SYMBOLS.join(",");
  const clientKey = `summary-vu-${__VU}-${__ITER}`;
  const url = `${BASE_URL}/api/futures/markets/summary/stream?symbols=${symbolsQuery}&clientKey=${clientKey}`;

  const params = {
    timeout: STREAM_TIMEOUT,
    headers: eventStreamHeaders(),
  };

  activeConnections.add(1);
  const startTime = Date.now();

  const res = http.get(url, params);

  const duration = Date.now() - startTime;
  connectionDurations.add(duration);
  activeConnections.add(-1);

  // Status 200 represents successful SSE connection handshake
  const success = check(res, {
    "summary stream handshake success": (r) => r.status === 200,
  });

  if (!success) {
    connectionFailures.add(1);
    sleep(2); // Back-off on failure
  } else {
    sleep(0.5); // Brief wait before reconnecting to hold consistent concurrency
  }
}

// ----------------------------------------------------
// B. Market Candle Stream Flow (Anonymous)
// ----------------------------------------------------
export function marketCandleStreamFlow() {
  const symbol = randomSymbol();
  const interval = "1m";
  const clientKey = `candle-vu-${__VU}-${__ITER}`;
  const url = `${BASE_URL}/api/futures/markets/${symbol}/candles/stream?interval=${interval}&clientKey=${clientKey}`;

  const params = {
    timeout: STREAM_TIMEOUT,
    headers: eventStreamHeaders(),
  };

  activeConnections.add(1);
  const startTime = Date.now();

  const res = http.get(url, params);

  const duration = Date.now() - startTime;
  connectionDurations.add(duration);
  activeConnections.add(-1);

  const success = check(res, {
    "candle stream handshake success": (r) => r.status === 200,
  });

  if (!success) {
    connectionFailures.add(1);
    sleep(2);
  } else {
    sleep(0.5);
  }
}

// ----------------------------------------------------
// C. Order Execution Stream Flow (Authenticated)
// ----------------------------------------------------
export function orderExecutionStreamFlow(data) {
  // --- Bind each VU to a setup-created JWT so stream load excludes signup/login writes. ---
  if (!orderStreamAccessToken) {
    const precreatedUser = precreatedUserForVu(data);
    orderStreamAccessToken = precreatedUser.accessToken;
  }

  const clientKey = `order-vu-${__VU}-${__ITER}`;
  const url = `${BASE_URL}/api/futures/orders/stream?clientKey=${clientKey}`;
  const streamHeaders = withAccessToken(eventStreamHeaders(), orderStreamAccessToken);

  const params = {
    timeout: STREAM_TIMEOUT,
    headers: streamHeaders,
  };

  activeConnections.add(1);
  const startTime = Date.now();

  const res = http.get(url, params);

  const duration = Date.now() - startTime;
  connectionDurations.add(duration);
  activeConnections.add(-1);

  const success = check(res, {
    "order stream handshake success": (r) => r.status === 200,
  });

  if (!success) {
    connectionFailures.add(1);
    sleep(2);
  } else {
    sleep(0.5);
  }
}
