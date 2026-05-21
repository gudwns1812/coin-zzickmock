import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";

// Dynamic custom performance indicators (KPIs)
const fullSessionTrend = new Trend("session_total_duration_ms");
const apiRequestFailures = new Counter("session_failures");

// k6 options: Ramping up to 40 VUs for extensive multi-domain testing
export const options = {
  stages: [
    { duration: "30s", target: 10 },  // Warm-up: ramping to 10 VUs
    { duration: "2m", target: 30 },   // Steady peak load: 30 VUs acting like real players
    { duration: "30s", target: 40 },  // Final pressure spike: 40 VUs
    { duration: "20s", target: 0 },   // Cool-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.015"],                 // Keep failure rate below 1.5%
    http_req_duration: ["p(95)<350"],               // 95% of requests under 350ms
    session_total_duration_ms: ["p(95)<8000"],      // Total user scenario loop within 8 seconds
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Base configurations
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");
const SYMBOLS = ["BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT"];
const INTERVALS = ["1m", "5m", "15m", "1h", "1D"];

// Data helpers
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

export default function () {
  const sessionStart = Date.now();
  const headers = { "Content-Type": "application/json", "Accept": "application/json" };

  // --- Step 1: Onboarding Session ---
  // Register and Login at the start of VUs lifetime
  if (__ITER === 0) {
    group("Phase 1: User Onboarding", function () {
      const username = `user_session_${__VU}_${randomString(4)}`;
      const password = "Password123!";
      
      // Duplication check
      http.post(`${BASE_URL}/api/futures/auth/duplicate`, JSON.stringify({ account: username }), { headers });
      
      // Register
      const regRes = http.post(`${BASE_URL}/api/futures/auth/register`, JSON.stringify({
        account: username,
        password,
        name: `Session VU ${__VU}`,
        nickname: `S_${__VU}_${randomString(4)}`,
        phoneNumber: "010-9999-9999",
        email: `${username}@session-test.com`,
      }), { headers });

      check(regRes, { "registration success": (r) => r.status === 200 });
      sleep(0.5);

      // Login
      const logRes = http.post(`${BASE_URL}/api/futures/auth/login`, JSON.stringify({ account: username, password }), { headers });
      check(logRes, { "login success": (r) => r.status === 200 });
    });
    sleep(1.0);
  }

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

    // 3. Spy on ranked trader if token exists
    if (targetToken) {
      const peekPayload = {
        targetToken,
        idempotencyKey: `uuid-${randomString(16)}`,
      };
      const peekRes = http.post(`${BASE_URL}/api/futures/position-peeks`, JSON.stringify(peekPayload), { headers });
      check(peekRes, { "spy position status check": (r) => r.status === 200 || r.status === 400 });
    }
  });

  sleep(randomRange(1.0, 2.0)); // Think time

  // --- Step 4: Futures Trading Simulation (Transactional, weight: High) ---
  let activePositions = [];
  group("Phase 4: Trading Operations", function () {
    // 1. Check account balance
    const accRes = http.get(`${BASE_URL}/api/futures/account/me`);
    let balance = 0;
    if (accRes.status === 200) {
      balance = accRes.json("data.availableBalance") || 0;
    }

    // Refill if empty
    if (balance < 200) {
      http.post(`${BASE_URL}/api/futures/account/me/refill`, null, { headers });
      sleep(0.5);
    }

    // 2. Place a market futures order (LONG or SHORT)
    const tradeSymbol = selectRandom(SYMBOLS);
    const side = selectRandom(["LONG", "SHORT"]);
    
    // Obtain last market price to calculate margin accurately
    let marketPrice = 65000;
    const tickerRes = http.get(`${BASE_URL}/api/futures/markets/${tradeSymbol}`);
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
    http.post(`${BASE_URL}/api/futures/orders/preview`, JSON.stringify(orderPayload), { headers });
    sleep(0.5);

    // Place
    const orderRes = http.post(`${BASE_URL}/api/futures/orders`, JSON.stringify(orderPayload), { headers });
    check(orderRes, { "futures order submitted ok": (r) => r.status === 200 || r.status === 400 });

    sleep(1.0);

    // 3. Query Active Positions & Apply TP/SL
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`);
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
      
      const tpslRes = http.patch(`${BASE_URL}/api/futures/positions/tpsl`, JSON.stringify(tpslPayload), { headers });
      check(tpslRes, { "tp/sl adjustment ok": (r) => r.status === 200 || r.status === 400 });
      
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
      const closeRes = http.post(`${BASE_URL}/api/futures/positions/close`, JSON.stringify(closePayload), { headers });
      check(closeRes, { "futures position closed ok": (r) => r.status === 200 });
    }
  });

  sleep(randomRange(1.0, 2.0)); // Think time

  // --- Step 5: Reward Shop & Community Engagement (Engagement, weight: Medium) ---
  group("Phase 5: Store & Community Engagements", function () {
    // 1. Inspect reward point inventory
    const pointsRes = http.get(`${BASE_URL}/api/futures/rewards/me`);
    check(pointsRes, { "check point assets ok": (r) => r.status === 200 });

    // 2. Fetch list of community discussions
    const postsRes = http.get(`${BASE_URL}/api/futures/community/posts?category=CHART_ANALYSIS&page=0&size=10`);
    const postsOk = check(postsRes, { "posts category view ok": (r) => r.status === 200 });

    if (postsOk) {
      const posts = postsRes.json("data.posts") || [];
      if (posts.length > 0) {
        const targetPost = selectRandom(posts);
        
        sleep(0.5);

        // View detail
        http.get(`${BASE_URL}/api/futures/community/posts/${targetPost.id}`);
        
        sleep(0.5);

        // Like the post
        http.post(`${BASE_URL}/api/futures/community/posts/${targetPost.id}/like`, null, { headers });

        sleep(0.5);

        // Add a friendly comment
        const commentPayload = { content: "Brilliant mock futures analysis. I opened a LONG position based on this!" };
        http.post(`${BASE_URL}/api/futures/community/posts/${targetPost.id}/comments`, JSON.stringify(commentPayload), { headers });
      }
    }
  });

  // Calculate session KPI
  fullSessionTrend.add(Date.now() - sessionStart);
  
  sleep(2.0); // Session loop gap
}
