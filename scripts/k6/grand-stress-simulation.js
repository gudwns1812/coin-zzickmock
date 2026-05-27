import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
  businessParams,
  canPurchaseShopItem,
  jsonHeaders,
  precreatedUserCount,
  precreatedUserForVu,
  setupPrecreatedUsers,
  supportedSymbolsFromEnv,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

// Global VU-local JWT storage (persisted across iterations of the same VU thread)
let vuToken = null;

const BASE_URL = (__ENV.BASE_URL || "http://host.docker.internal:18080").replace(/\/$/, "");
const SYMBOLS = supportedSymbolsFromEnv();

// Performance trends for critical user interactions under stress
const orderExecTime = new Trend("grand_order_execution_ms");
const positionCloseTime = new Trend("grand_position_close_ms");
const marketReadTime = new Trend("grand_market_read_ms");
const failedOps = new Counter("grand_failed_operations");

export const options = {
  stages: [
    { duration: "30s", target: 150 },  // Ramp-up: 0 to 150 VUs
    { duration: "1m", target: 350 },   // Ramp-up: 150 to 350 VUs
    { duration: "2m", target: 500 },   // Ramp-up: 350 to 500 VUs (Consistent Peak)
    { duration: "1m", target: 500 },   // Sustained peak load at 500 VUs
    { duration: "30s", target: 0 },    // Cool-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.05"],    // System-wide error rate < 5%
    http_req_duration: ["p(95)<500"],  // 95% of API requests completed under 500ms
  },
  noCookiesReset: true,
};

export function setup() {
  return setupPrecreatedUsers(BASE_URL, {
    prefix: "k6_grand",
    count: precreatedUserCount(500),
  });
}

function randomString(length = 6) {
  const chars = "abcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

// Helper: Random choice
function selectRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export default function (data) {
  // --- 1. STATE INITIALIZATION (First Iteration or when JWT is missing) ---
  if (!vuToken) {
    const precreatedUser = precreatedUserForVu(data);
    vuToken = precreatedUser.accessToken;
  }

  // Explicitly build standard headers with the JWT cookie for all authenticated requests.
  const headers = withAccessToken(jsonHeaders(), vuToken);

  // --- VU STATE VARIABLES ---
  let balance = 0;
  let activePositions = [];
  let openOrders = [];

  // --- 2. MULTI-DOMAIN FLOW ACTIONS (Tight Sleep to generate heavy RPS) ---

  // Action A: Market Inspection & Chart Candle Load
  group("A. Market & Chart Data Inspection", function () {
    const symbol = selectRandom(SYMBOLS);
    const startRead = Date.now();
    
    // Ticker last price read
    const priceRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}`);
    // Historical candles load (Simulating user entering chart room)
    const candlesRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}/candles?interval=1m&limit=100`);

    const readOk = check(priceRes, {
      "market status 200": (r) => r.status === 200,
    }) && check(candlesRes, {
      "candles status 200": (r) => r.status === 200,
    });

    marketReadTime.add(Date.now() - startRead);

    if (!readOk) {
      failedOps.add(1);
    }
  });

  sleep(2.0); // Tight Think Time

  // Action B: Social Leaderboard Searching & Peeking
  group("B. Leaderboard Exploration & Position Peeking", function () {
    // 1. Browse leaderboard (Weekly)
    const boardRes = http.get(`${BASE_URL}/api/futures/leaderboard?mode=profitRate&limit=20`, { headers: headers });
    const boardOk = check(boardRes, {
      "leaderboard status 200": (r) => r.status === 200,
    });

    if (!boardOk) {
      console.log(`[B. LEADERBOARD FAIL] status=${boardRes.status} body=${boardRes.body}`);
    }

    if (boardOk) {
      const entries = boardRes.json("data.entries") || [];
      if (entries.length > 0) {
        // Pick a ranker and view their public positioning
        const targetRanker = selectRandom(entries);
        sleep(0.1);

        // Peek target ranker's position (Stateful action)
        const peekRes = http.post(
          `${BASE_URL}/api/futures/position-peeks`,
          JSON.stringify({
            targetToken: targetRanker.targetToken,
            idempotencyKey: `uuid-${randomString(16)}`,
          }),
          businessParams(headers)
        );

        const peekOk = check(peekRes, {
          "peek status 200 or 400/403": (r) => r.status === 200 || r.status === 400 || r.status === 403,
        });

        if (!peekOk) {
          console.log(`[B. PEEK FAIL] status=${peekRes.status} body=${peekRes.body}`);
        }
      }
    }
  });

  sleep(1.5);

  // Action C: Balance Audit & Refill
  group("C. Wallet Audit & Refill", function () {
    const accRes = http.get(`${BASE_URL}/api/futures/account/me`, { headers: headers });
    const accOk = check(accRes, {
      "wallet check status 200": (r) => r.status === 200,
    });

    if (!accOk) {
      console.log(`[C. WALLET CHECK FAIL] status=${accRes.status} body=${accRes.body}`);
    }

    if (accOk) {
      balance = accRes.json("data.available") || 0;
      if (balance < 200) {
        sleep(0.1);
        const refillRes = http.post(`${BASE_URL}/api/futures/account/me/refill`, null, businessParams(headers));
        const refillOk = check(refillRes, {
          "refill processed": (r) => r.status === 200 || r.status === 400,
        });
        if (!refillOk) {
          console.log(`[C. REFILL FAIL] status=${refillRes.status} body=${refillRes.body}`);
        }
      }
    }
  });

  sleep(2.0);

  // Action D: Order Submission (Long/Short)
  group("D. Active Futures Order Submission", function () {
    const symbol = selectRandom(SYMBOLS);
    const positionSide = selectRandom(["LONG", "SHORT"]);
    const orderType = selectRandom(["MARKET", "LIMIT"]);
    const marginMode = "ISOLATED";
    const leverage = selectRandom([10, 20]);
    const quantity = selectRandom([0.01, 0.03]);

    let currentPrice = 65000;
    const priceRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}`);
    if (priceRes.status === 200 && priceRes.json("data.lastPrice")) {
      currentPrice = priceRes.json("data.lastPrice");
    }

    const limitPrice = orderType === "LIMIT" 
      ? (positionSide === "LONG" ? currentPrice * 0.995 : currentPrice * 1.005) 
      : null;

    const orderPayload = {
      symbol,
      positionSide,
      orderType,
      marginMode,
      leverage,
      quantity,
      limitPrice,
    };

    // Submit order
    const orderStart = Date.now();
    const orderRes = http.post(
      `${BASE_URL}/api/futures/orders`,
      JSON.stringify(orderPayload),
      businessParams(headers)
    );

    const orderOk = check(orderRes, {
      "order status 200 or 400": (r) => r.status === 200 || r.status === 400,
    });

    orderExecTime.add(Date.now() - orderStart);

    if (!orderOk) {
      failedOps.add(1);
      console.log(`[D. ORDER SUBMIT FAIL] status=${orderRes.status} body=${orderRes.body}`);
    }
  });

  sleep(1.5);

  // Action E: Position & Open Orders Management
  group("E. Open Positions & Active Orders Management", function () {
    // 1. Audit active positions
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`, { headers: headers });
    const posOk = check(posRes, {
      "positions fetch successful": (r) => r.status === 200,
    });

    if (!posOk) {
      console.log(`[E. POSITIONS FETCH FAIL] status=${posRes.status} body=${posRes.body}`);
    }

    if (posOk) {
      activePositions = posRes.json("data") || [];
      
      // Randomly close some active positions to release margins (30% probability)
      if (activePositions.length > 0 && Math.random() < 0.3) {
        const targetPos = selectRandom(activePositions);
        const closePayload = {
          symbol: targetPos.symbol,
          positionSide: targetPos.positionSide,
          quantity: targetPos.quantity,
          marginMode: targetPos.marginMode,
          orderType: "MARKET",
          limitPrice: null,
        };

        sleep(0.1);
        const closeStart = Date.now();
        const closeRes = http.post(
          `${BASE_URL}/api/futures/positions/close`,
          JSON.stringify(closePayload),
          businessParams(headers)
        );

        const closeOk = check(closeRes, {
          "position close processed": (r) => r.status === 200 || r.status === 400,
        });

        if (!closeOk) {
          console.log(`[E. POSITION CLOSE FAIL] status=${closeRes.status} body=${closeRes.body}`);
        }

        positionCloseTime.add(Date.now() - closeStart);
      }
    }

    // 2. Audit open limit orders and cancel randomly to keep system clean
    const openRes = http.get(`${BASE_URL}/api/futures/orders/open`, { headers: headers });
    const openOk = check(openRes, {
      "open orders fetch successful": (r) => r.status === 200,
    });

    if (!openOk) {
      console.log(`[E. OPEN ORDERS FETCH FAIL] status=${openRes.status} body=${openRes.body}`);
    }

    if (openOk) {
      openOrders = openRes.json("data") || [];
      if (openOrders.length > 0 && Math.random() < 0.4) {
        const targetOrder = selectRandom(openOrders);
        sleep(0.1);

        const cancelRes = http.post(
          `${BASE_URL}/api/futures/orders/${targetOrder.orderId}/cancel`,
          null,
          { headers }
        );
        const cancelOk = check(cancelRes, { "order cancel success": (r) => r.status === 200 });
        if (!cancelOk) {
          console.log(`[E. ORDER CANCEL FAIL] status=${cancelRes.status} body=${cancelRes.body}`);
        }
      }
    }
  });

  sleep(2.0);

  // Action F: Reward Point Shop Purchases
  group("F. Reward Store Interaction", function () {
    const rewardsRes = http.get(`${BASE_URL}/api/futures/rewards/me`, { headers });
    const rewardPoint = rewardsRes.status === 200 ? rewardsRes.json("data.rewardPoint") || 0 : 0;

    // Fetch product list
    const shopRes = http.get(`${BASE_URL}/api/futures/shop/items`, { headers: headers });
    const shopOk = check(shopRes, {
      "shop products fetch status 200": (r) => r.status === 200,
    });

    if (!shopOk) {
      console.log(`[F. SHOP ITEMS FETCH FAIL] status=${shopRes.status} body=${shopRes.body}`);
    }

    if (shopOk) {
      const products = shopRes.json("data") || [];
      const purchasableProducts = products.filter((item) => canPurchaseShopItem(item, rewardPoint));
      // Buy random reward if products available and user has points (15% probability)
      if (purchasableProducts.length > 0 && Math.random() < 0.15) {
        const targetProduct = selectRandom(purchasableProducts);
        const code = targetProduct.code;
        sleep(0.1);

        const buyRes = http.post(
          `${BASE_URL}/api/futures/shop/items/${code}/purchase`,
          null,
          businessParams(headers)
        );

        const buyOk = check(buyRes, {
          "reward purchase processed": (r) => r.status === 200 || r.status === 400,
        });

        if (!buyOk) {
          console.log(`[F. REWARD PURCHASE FAIL] status=${buyRes.status} body=${buyRes.body}`);
        }
      }
    }
  });

  sleep(3.0); // Tight loop end delay
}
