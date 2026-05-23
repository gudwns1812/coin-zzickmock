import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter } from "k6/metrics";
import { supportedSymbolsFromEnv } from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

// Custom Trends to track database lookup times and leaderboard searches
const marketSummaryTrend = new Trend("market_summary_duration_ms");
const candlesRetrievalTrend = new Trend("candles_retrieval_duration_ms");
const leaderboardSearchTrend = new Trend("leaderboard_search_duration_ms");
const readFailures = new Counter("market_leaderboard_failures");

// k6 options: high-throughput, ramping up to 100 VUs
export const options = {
  scenarios: {
    ticker_polling: {
      executor: "ramping-vus",
      startVUs: 10,
      stages: [
        { duration: "20s", target: 50 },  // Ramp up
        { duration: "1m", target: 100 },  // Heavy sustained load
        { duration: "20s", target: 0 },    // Cool down
      ],
      gracefulRampDown: "5s",
      exec: "tickerPollingFlow",
    },
    candles_reading: {
      executor: "ramping-vus",
      startVUs: 5,
      stages: [
        { duration: "20s", target: 30 },  // Ramp up
        { duration: "1m", target: 50 },   // Heavy sustained load
        { duration: "20s", target: 0 },    // Cool down
      ],
      gracefulRampDown: "5s",
      exec: "candleRetrievalFlow",
    },
    leaderboard_searching: {
      executor: "ramping-vus",
      startVUs: 2,
      stages: [
        { duration: "20s", target: 15 },  // Ramp up
        { duration: "1m", target: 30 },   // Dynamic substring sorting load
        { duration: "20s", target: 0 },    // Cool down
      ],
      gracefulRampDown: "5s",
      exec: "leaderboardBrowsingFlow",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],            // Error rate must be less than 1%
    "http_req_duration{scenario:ticker_polling}": ["p(95)<150"], // Ticker polling must be very fast (<150ms)
    "http_req_duration{scenario:candles_reading}": ["p(95)<300"], // Candle retrieval should be stable (<300ms)
    "http_req_duration{scenario:leaderboard_searching}": ["p(95)<500"], // Leaderboard & search can have in-memory sort latency
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Base configuration
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");

// Supported symbols, intervals, and search prefixes to simulate client requests
const SYMBOLS = supportedSymbolsFromEnv();
const INTERVALS = ["1m", "3m", "5m", "15m", "1h", "4h", "1D", "1W", "1M"];
const SEARCH_QUERIES = ["coin", "king", "trade", "user", "alpha", "test", "trader"];

// Helper for selecting random items
function selectRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

// Helper for random number range
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// 1. Ticker Polling Flow (Highly performant, backed by Redis Market Snapshot)
export function tickerPollingFlow() {
  group("Ticker Polling Group", function () {
    const startTime = Date.now();

    // 1. Fetch entire market summaries
    const summariesRes = http.get(`${BASE_URL}/api/futures/markets`);
    const checkAll = check(summariesRes, {
      "all tickers status 200": (r) => r.status === 200,
      "all tickers success true": (r) => r.json("success") === true,
      "has market data array": (r) => Array.isArray(r.json("data")),
    });

    if (!checkAll) {
      readFailures.add(1);
    }

    sleep(1.0); // Real clients typically poll every 1-3 seconds

    // 2. Fetch specific ticker summary
    const randomSymbol = selectRandom(SYMBOLS);
    const tickerRes = http.get(`${BASE_URL}/api/futures/markets/${randomSymbol}`);
    const checkSingle = check(tickerRes, {
      "single ticker status 200": (r) => r.status === 200,
      "single ticker has symbol": (r) => r.json("data.symbol") === randomSymbol,
    });

    if (!checkSingle) {
      readFailures.add(1);
    }

    marketSummaryTrend.add(Date.now() - startTime);
  });
}

// 2. Candle History Retrieval (Hits DB & Candle Appender caches)
export function candleRetrievalFlow() {
  group("Candle History Retrieval Group", function () {
    const startTime = Date.now();

    const symbol = selectRandom(SYMBOLS);
    const interval = selectRandom(INTERVALS);
    const limit = randomInt(50, 180); // Mix limits to test DB index range scan variations

    // Dynamic historical candle query
    const candlesUrl = `${BASE_URL}/api/futures/markets/${symbol}/candles?interval=${interval}&limit=${limit}`;
    const candlesRes = http.get(candlesUrl);

    const checkCandles = check(candlesRes, {
      "candles status 200": (r) => r.status === 200,
      "candles success true": (r) => r.json("success") === true,
      "candles data array returned": (r) => Array.isArray(r.json("data")),
    });

    candlesRetrievalTrend.add(Date.now() - startTime);

    if (!checkCandles) {
      readFailures.add(1);
    }

    sleep(randomInt(1, 3)); // Simulate user browsing charts
  });
}

// 3. Leaderboard Browsing and Searching (Stresses DB projections, Java in-memory sorting, and JWT Cryptography)
export function leaderboardBrowsingFlow() {
  group("Leaderboard Exploration Group", function () {
    const startTime = Date.now();

    // 1. Browse the global ROI / PNL rankings (Redis Snapshot backed)
    const mode = selectRandom(["profitRate", "walletBalance"]);
    const leaderboardRes = http.get(`${BASE_URL}/api/futures/leaderboard?mode=${mode}&limit=20`);
    
    const checkLeaderboard = check(leaderboardRes, {
      "leaderboard status 200": (r) => r.status === 200,
      "leaderboard data entries exist": (r) => Array.isArray(r.json("data.entries")),
    });

    if (!checkLeaderboard) {
      readFailures.add(1);
    }

    sleep(1.5);

    // 2. Substring Member search (Extremely high-risk, loads entire database rows into memory and sorts in Java)
    const searchQuery = selectRandom(SEARCH_QUERIES);
    const searchRes = http.get(`${BASE_URL}/api/futures/leaderboard/search?query=${searchQuery}&limit=10`);
    
    const checkSearch = check(searchRes, {
      "search status 200": (r) => r.status === 200,
      "search results format valid": (r) => Array.isArray(r.json("data")),
    });

    leaderboardSearchTrend.add(Date.now() - startTime);

    if (!checkSearch) {
      readFailures.add(1);
    }

    sleep(2.0); // Simulate user inspecting ranking details
  });
}
