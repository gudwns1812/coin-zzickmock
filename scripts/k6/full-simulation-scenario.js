import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend } from "k6/metrics";
import {
  businessParams,
  jsonHeaders,
  paramsWithHeaders,
  positiveIntFromEnv,
  precreatedUserCount,
  precreatedUserForVu,
  setupPrecreatedUsers,
  supportedSymbolsFromEnv,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

// Dynamic custom performance indicators (KPIs)
const fullSessionTrend = new Trend("session_total_duration_ms");

const TARGET_VUS = positiveIntFromEnv("FULL_SIMULATION_TARGET_VUS", 500);
const WARMUP_VUS = Math.max(1, Math.ceil(TARGET_VUS * 0.2));
const RAMP_VUS = Math.max(WARMUP_VUS, Math.ceil(TARGET_VUS * 0.6));
const TRADING_FLOW_RATE = probabilityFromEnv("FULL_SIMULATION_TRADING_RATE", 0.1);
const SOCIAL_PEEK_RATE = probabilityFromEnv("FULL_SIMULATION_SOCIAL_PEEK_RATE", 0.1);
const COMMUNITY_WRITE_RATE = probabilityFromEnv("FULL_SIMULATION_COMMUNITY_WRITE_RATE", 0.03);

// k6 options: default 500 VUs with gradual ramping, sustained peak, and cooldown.
// The peak can still be overridden with FULL_SIMULATION_TARGET_VUS when needed.
export const options = {
  setupTimeout: "5m",
  stages: [
    { duration: "1m", target: WARMUP_VUS },  // Warm-up: initialize accounts, auth cookies, caches, and DB pools
    { duration: "2m", target: RAMP_VUS },    // Ramp: increase mixed read/write load without a sudden thundering herd
    { duration: "3m", target: TARGET_VUS },  // Peak ramp: reach the full 500 VU target by default
    { duration: "5m", target: TARGET_VUS },  // Sustained peak: keep enough time for scheduler/cache/DB symptoms to surface
    { duration: "2m", target: 0 },           // Cool-down: let in-flight position/community writes drain
  ],
  thresholds: {
    http_req_failed: ["rate<0.03"],                 // Full journey stress allows controlled business conflicts/fallbacks
    http_req_duration: ["p(95)<1000"],              // Mixed endpoint p95 budget for 500 VU stress
    session_total_duration_ms: ["p(95)<25000"],     // Includes deliberate think time across the full product journey
  },
  noCookiesReset: true,
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Base configurations
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");
const SYMBOLS = supportedSymbolsFromEnv();
const INTERVALS = ["1m", "5m", "15m", "1h", "1D"];
let vuAccessToken = null;

export function setup() {
  return setupPrecreatedUsers(BASE_URL, {
    prefix: "k6_journey",
    count: Math.max(precreatedUserCount(TARGET_VUS), TARGET_VUS),
  });
}

function randomString(length) {
  const chars = "abcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function selectRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function randomRange(min, max) {
  return Math.random() * (max - min) + min;
}

function probabilityFromEnv(envName, fallback) {
  const parsed = Number.parseFloat(__ENV[envName]);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  return Math.min(1, Math.max(0, parsed));
}

function shouldRun(rate) {
  return Math.random() < rate;
}

function logUnexpectedResponse(operation, response, expectedStatuses, context = {}) {
  if (expectedStatuses.includes(response.status)) {
    return true;
  }

  const rawBody = response.body === null || response.body === undefined ? "" : String(response.body);
  const compactBody = rawBody.replace(/\s+/g, " ").slice(0, 500);
  console.log(JSON.stringify({
    event: "full_simulation.unexpected_response",
    operation,
    status: response.status,
    expectedStatuses,
    vu: __VU,
    iter: __ITER,
    context,
    body: compactBody,
  }));
  return false;
}

export default function (data) {
  const sessionStart = Date.now();
  const headers = jsonHeaders();

  // --- Step 1: Onboarding Flow ---
  // Use setup-created accounts so measured iterations focus on product flows, not onboarding writes.
  if (!vuAccessToken) {
    const precreatedUser = precreatedUserForVu(data);
    vuAccessToken = precreatedUser.accessToken;
  }

  const authHeaders = withAccessToken(headers, vuAccessToken);

  // --- Step 2: Browsing Market Tickers & Charts (Read-heavy, weight: High) ---
  group("Phase 2: Market Browsing & Chart Analysis", function () {
    // 1. Fetch all tickers (simulate landing page)
    const tickRes = http.get(`${BASE_URL}/api/futures/markets`);
    check(tickRes, { "get all markets ok": (r) => r.status === 200 });
    
    sleep(0.5);

    // 2. Look at a specific symbol
    const symbol = selectRandom(SYMBOLS);
    const tickerRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}`);
    check(tickerRes, { "get ticker ok": (r) => r.status === 200 });

    sleep(0.5);

    // 3. Request candles (browsing chart)
    const interval = selectRandom(INTERVALS);
    const candlesRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}/candles?interval=${interval}&limit=120`);
    check(candlesRes, { "get candles ok": (r) => r.status === 200 });
  });

  sleep(randomRange(1.0, 2.0)); // Think time

  // --- Step 3: Social Leaderboard & Peeking (Social, weight: Medium) ---
  let targetToken = null;
  group("Phase 3: Leaderboard & Social Peeking", function () {
    // 1. Check ROI global leaderboard
    const boardRes = http.get(`${BASE_URL}/api/futures/leaderboard?mode=profitRate&limit=10`);
    const boardOk = check(boardRes, { "get leaderboard ok": (r) => r.status === 200 });

    if (boardOk) {
      const entries = boardRes.json("data.entries") || [];
      const peekingTargets = entries.filter((e) => e.targetToken !== null);
      if (peekingTargets.length > 0) {
        targetToken = selectRandom(peekingTargets).targetToken;
      }
    }

    sleep(1.0);

    // 2. Perform a ranked trader search
    const searchQueries = ["coin", "king", "trade", "alpha"];
    const searchRes = http.get(`${BASE_URL}/api/futures/leaderboard/search?query=${selectRandom(searchQueries)}&limit=5`);
    check(searchRes, { "leaderboard search ok": (r) => r.status === 200 });

    sleep(0.5);

    // 3. Spy on ranked trader if token exists. At 500 VUs, only a realistic slice performs paid/social writes.
    if (targetToken && shouldRun(SOCIAL_PEEK_RATE)) {
      const peekPayload = {
        targetToken,
        idempotencyKey: `uuid-${randomString(16)}`,
      };
      const peekRes = http.post(`${BASE_URL}/api/futures/position-peeks`, JSON.stringify(peekPayload), businessParams(authHeaders));
      check(peekRes, { "spy position status check": (r) => r.status === 200 || r.status === 400 || r.status === 403 });
    }
  });

  sleep(randomRange(1.0, 2.0)); // Think time

  // --- Step 4: Futures Trading Simulation (Transactional, weight: High) ---
  let activePositions = [];
  group("Phase 4: Trading Operations", function () {
    // 1. Check account balance
    const accRes = http.get(`${BASE_URL}/api/futures/account/me`, paramsWithHeaders(authHeaders));
    logUnexpectedResponse("account_summary", accRes, [200]);
    let balance = 0;
    if (accRes.status === 200) {
      balance = accRes.json("data.available") || 0;
    }

    // Refill if empty
    if (balance < 200) {
      const refillRes = http.post(`${BASE_URL}/api/futures/account/me/refill`, null, businessParams(authHeaders));
      logUnexpectedResponse("account_refill", refillRes, [200, 400]);
      sleep(0.5);
    }

    if (!shouldRun(TRADING_FLOW_RATE)) {
      sleep(randomRange(1.5, 3.0));
      return;
    }

    // 2. Place a market futures order (LONG or SHORT)
    const tradeSymbol = selectRandom(SYMBOLS);
    const side = selectRandom(["LONG", "SHORT"]);
    
    // Obtain last market price to calculate margin accurately
    let marketPrice = 65000;
    const tickerRes = http.get(`${BASE_URL}/api/futures/markets/${tradeSymbol}`);
    logUnexpectedResponse("trade_market_ticker", tickerRes, [200], { symbol: tradeSymbol });
    if (tickerRes.status === 200 && tickerRes.json("data.lastPrice")) {
      marketPrice = tickerRes.json("data.lastPrice");
    }

    const orderPayload = {
      symbol: tradeSymbol,
      positionSide: side,
      orderType: "MARKET",
      marginMode: "ISOLATED",
      leverage: 10,
      quantity: tradeSymbol === "BTCUSDT" ? 0.01 : 0.1,
      limitPrice: null,
    };

    // Preview
    const previewRes = http.post(`${BASE_URL}/api/futures/orders/preview`, JSON.stringify(orderPayload), businessParams(authHeaders));
    logUnexpectedResponse("order_preview", previewRes, [200, 400], { symbol: tradeSymbol, side });
    sleep(0.5);

    // Place
    const orderRes = http.post(`${BASE_URL}/api/futures/orders`, JSON.stringify(orderPayload), businessParams(authHeaders));
    const orderOk = check(orderRes, { "futures order submitted ok": (r) => r.status === 200 || r.status === 400 });
    if (!orderOk) {
      logUnexpectedResponse("order_submit", orderRes, [200, 400], { symbol: tradeSymbol, side });
    }

    sleep(1.0);

    // 3. Query Active Positions & Apply TP/SL
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`, paramsWithHeaders(authHeaders));
    logUnexpectedResponse("positions_query", posRes, [200]);
    if (posRes.status === 200 && Array.isArray(posRes.json("data"))) {
      activePositions = posRes.json("data") || [];
    }

    if (activePositions.length > 0) {
      const pos = selectRandom(activePositions);
      sleep(0.5);

      // Patch take-profit and stop-loss levels
      const tpslPayload = {
        symbol: pos.symbol,
        positionSide: pos.positionSide,
        marginMode: pos.marginMode,
        takeProfitPrice: pos.positionSide === "LONG" ? pos.entryPrice * 1.15 : pos.entryPrice * 0.85,
        stopLossPrice: pos.positionSide === "LONG" ? pos.entryPrice * 0.85 : pos.entryPrice * 1.15,
      };
      
      const tpslRes = http.patch(`${BASE_URL}/api/futures/positions/tpsl`, JSON.stringify(tpslPayload), businessParams(authHeaders));
      const tpslOk = check(tpslRes, { "tp/sl adjustment ok": (r) => r.status === 200 || r.status === 400 });
      if (!tpslOk) {
        logUnexpectedResponse("position_tpsl", tpslRes, [200, 400], {
          symbol: pos.symbol,
          side: pos.positionSide,
          marginMode: pos.marginMode,
        });
      }
      
      sleep(1.0);

      // Close the position immediately to simulate exit
      const closePayload = {
        symbol: pos.symbol,
        positionSide: pos.positionSide,
        marginMode: pos.marginMode,
        quantity: pos.quantity,
        orderType: "MARKET",
        limitPrice: null,
      };
      const closeRes = http.post(`${BASE_URL}/api/futures/positions/close`, JSON.stringify(closePayload), businessParams(authHeaders));
      const closeOk = check(closeRes, { "futures position closed ok": (r) => r.status === 200 || r.status === 400 });
      if (!closeOk) {
        logUnexpectedResponse("position_close", closeRes, [200, 400], {
          symbol: pos.symbol,
          side: pos.positionSide,
          marginMode: pos.marginMode,
        });
      }
    }
  });

  sleep(randomRange(1.0, 2.0)); // Think time

  // --- Step 5: Reward Shop & Community Engagement (Engagement, weight: Medium) ---
  group("Phase 5: Store & Community Engagements", function () {
    // 1. Inspect reward point inventory
    const pointsRes = http.get(`${BASE_URL}/api/futures/rewards/me`, paramsWithHeaders(authHeaders));
    check(pointsRes, { "check point assets ok": (r) => r.status === 200 });

    // 2. Fetch list of community discussions
    const postsRes = http.get(
      `${BASE_URL}/api/futures/community/posts?category=CHART_ANALYSIS&page=0&size=10`,
      paramsWithHeaders(authHeaders)
    );
    const postsOk = check(postsRes, { "posts category view ok": (r) => r.status === 200 });

    if (postsOk) {
      const posts = postsRes.json("data.posts") || [];
      if (posts.length > 0) {
        const targetPost = selectRandom(posts);
        
        sleep(0.5);

        // View detail
        http.get(`${BASE_URL}/api/futures/community/posts/${targetPost.id}`, paramsWithHeaders(authHeaders));
        
        sleep(0.5);

        if (shouldRun(COMMUNITY_WRITE_RATE)) {
          // Like the post
          http.post(`${BASE_URL}/api/futures/community/posts/${targetPost.id}/like`, null, { headers: authHeaders });

          sleep(0.5);

          // Add a friendly comment
          const commentPayload = { content: "Brilliant mock futures analysis. I opened a LONG position based on this!" };
          http.post(
            `${BASE_URL}/api/futures/community/posts/${targetPost.id}/comments`,
            JSON.stringify(commentPayload),
            { headers: authHeaders }
          );
        }
      }
    }
  });

  // Calculate journey KPI
  fullSessionTrend.add(Date.now() - sessionStart);
  
  sleep(2.0); // Journey loop gap
}
