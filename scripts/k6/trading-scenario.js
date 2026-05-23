import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
  businessParams,
  jsonHeaders,
  paramsWithHeaders,
  precreatedUserCount,
  precreatedUserForVu,
  setupPrecreatedUsers,
  supportedSymbolsFromEnv,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

// Custom Performance Metrics
const orderPlacementTrend = new Trend("trading_order_placement_ms");
const positionCloseTrend = new Trend("trading_position_close_ms");
const peekActionTrend = new Trend("trading_social_peek_ms");
const failedTransactions = new Counter("trading_failed_transactions");

// k6 options: Ramping up to 20 concurrent stateful traders to avoid over-colliding order margins
export const options = {
  stages: [
    { duration: "20s", target: 5 },   // Warm-up: 0 to 5 VUs
    { duration: "1m", target: 15 },   // Normal load: 15 active traders executing stateful logic
    { duration: "20s", target: 0 },    // Cool-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.02"],                 // Error rate < 2% (trading can fail on invalid margins, etc.)
    http_req_duration: ["p(95)<300"],               // 95% of requests under 300ms
    trading_order_placement_ms: ["p(95)<400"],
    trading_position_close_ms: ["p(95)<400"],
    trading_social_peek_ms: ["p(95)<400"],
  },
  noCookiesReset: true,
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Base environment setups
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");
const SYMBOLS = supportedSymbolsFromEnv();
let vuAccessToken = null;

export function setup() {
  return setupPrecreatedUsers(BASE_URL, {
    prefix: "k6_trader",
    count: precreatedUserCount(250),
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

export default function (data) {
  // --- STATE INITIALIZATION FOR VU ---
  // Use setup-created accounts so measured iterations do not include signup/login write load.
  if (!vuAccessToken) {
    const precreatedUser = precreatedUserForVu(data);
    vuAccessToken = precreatedUser.accessToken;
  }

  const authHeaders = withAccessToken(jsonHeaders(), vuAccessToken);

  let balance = 0;
  let activePositions = [];
  let openOrders = [];

  // Group 1: Balance Inspection and Refill
  group("1. Balance Verification", function () {
    const accRes = http.get(`${BASE_URL}/api/futures/account/me`, paramsWithHeaders(authHeaders));
    const accOk = check(accRes, {
      "account fetch status 200": (r) => r.status === 200,
      "has available field": (r) => typeof r.json("data.available") === "number",
    });

    if (accOk) {
      balance = accRes.json("data.available");
    } else {
      failedTransactions.add(1);
    }

    // Refill balance if funds are dangerously low (< 100 USDT)
    if (balance < 100) {
      sleep(0.5);
      const refillRes = http.post(`${BASE_URL}/api/futures/account/me/refill`, null, businessParams(authHeaders));
      check(refillRes, {
        "refill status 200 or 400": (r) => r.status === 200 || r.status === 400, // 400 means daily limit reached
      });
    }
  });

  sleep(1);

  // Group 2: Position State Audit
  group("2. Positions Audit", function () {
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`, paramsWithHeaders(authHeaders));
    const posOk = check(posRes, {
      "positions fetch status 200": (r) => r.status === 200,
      "positions list parsed": (r) => Array.isArray(r.json("data")),
    });

    if (posOk) {
      activePositions = posRes.json("data") || [];
    } else {
      failedTransactions.add(1);
    }
  });

  sleep(1);

  // Group 3: Preview and Order Submission (Create Position)
  group("3. Order Entry (Buy/Sell)", function () {
    const symbol = selectRandom(SYMBOLS);
    const positionSide = selectRandom(["LONG", "SHORT"]);
    const orderType = selectRandom(["MARKET", "LIMIT"]);
    const marginMode = "ISOLATED";
    const leverage = selectRandom([5, 10, 20]);
    const quantity = selectRandom([0.01, 0.02, 0.05]);

    // Query current price of symbol to set a realistic limit price
    let currentPrice = 65000; // fallback
    const priceRes = http.get(`${BASE_URL}/api/futures/markets/${symbol}`);
    if (priceRes.status === 200 && priceRes.json("data.lastPrice")) {
      currentPrice = priceRes.json("data.lastPrice");
    }

    const limitPrice = orderType === "LIMIT" 
      ? (positionSide === "LONG" ? currentPrice * 0.99 : currentPrice * 1.01) 
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

    // 1. Preview order
    const previewRes = http.post(
      `${BASE_URL}/api/futures/orders/preview`,
      JSON.stringify(orderPayload),
      businessParams(authHeaders)
    );
    check(previewRes, {
      "preview status 200 or 400": (r) => r.status === 200 || r.status === 400,
    });

    sleep(0.5);

    // 2. Submit actual order
    const orderStartTime = Date.now();
    const orderRes = http.post(
      `${BASE_URL}/api/futures/orders`,
      JSON.stringify(orderPayload),
      businessParams(authHeaders)
    );

    const orderOk = check(orderRes, {
      "order status 200 or 400": (r) => r.status === 200 || r.status === 400,
      "order exec success": (r) => r.status === 400 || r.json("success") === true,
    });

    orderPlacementTrend.add(Date.now() - orderStartTime);

    if (!orderOk) {
      failedTransactions.add(1);
    }
  });

  sleep(1);

  // Group 4: Manage Open Orders (If any limit order was created)
  group("4. Open Orders Modification", function () {
    const openRes = http.get(`${BASE_URL}/api/futures/orders/open`, paramsWithHeaders(authHeaders));
    const openOk = check(openRes, {
      "open orders status 200": (r) => r.status === 200,
      "open orders list parsed": (r) => Array.isArray(r.json("data")),
    });

    if (openOk) {
      openOrders = openRes.json("data") || [];
      if (openOrders.length > 0) {
        const targetOrder = selectRandom(openOrders);
        sleep(0.5);

        const action = selectRandom(["MODIFY", "CANCEL"]);
        if (action === "MODIFY") {
          const modPayload = {
            limitPrice: targetOrder.limitPrice * selectRandom([0.995, 1.005]),
          };
          const modRes = http.post(
            `${BASE_URL}/api/futures/orders/${targetOrder.orderId}/modify`,
            JSON.stringify(modPayload),
            { headers: authHeaders }
          );
          check(modRes, { "modify status 200": (r) => r.status === 200 });
        } else {
          const cancelRes = http.post(
            `${BASE_URL}/api/futures/orders/${targetOrder.orderId}/cancel`,
            null,
            { headers: authHeaders }
          );
          check(cancelRes, { "cancel status 200": (r) => r.status === 200 });
        }
      }
    }
  });

  sleep(1.5);

  // Group 5: Active Position Adjustments & Social Peek
  group("5. Position Management & Social Peek", function () {
    // Refresh position states
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`, paramsWithHeaders(authHeaders));
    if (posRes.status === 200 && Array.isArray(posRes.json("data"))) {
      activePositions = posRes.json("data") || [];
    }

    if (activePositions.length > 0) {
      const position = selectRandom(activePositions);
      sleep(0.5);

      // Action A: Adjust Leverage / TP-SL
      const adjustType = selectRandom(["LEVERAGE", "TPSL"]);
      if (adjustType === "LEVERAGE") {
        const levPayload = {
          symbol: position.symbol,
          positionSide: position.positionSide,
          marginMode: position.marginMode,
          leverage: selectRandom([10, 15, 25]),
        };
        const levRes = http.patch(
          `${BASE_URL}/api/futures/positions/leverage`,
          JSON.stringify(levPayload),
          businessParams(authHeaders)
        );
        check(levRes, {
          "patch leverage status 200 or 400": (r) => r.status === 200 || r.status === 400,
        });
      } else {
        const currentEntryPrice = position.entryPrice;
        const isLong = position.positionSide === "LONG";
        
        const tpslPayload = {
          symbol: position.symbol,
          positionSide: position.positionSide,
          marginMode: position.marginMode,
          takeProfitPrice: isLong ? currentEntryPrice * 1.1 : currentEntryPrice * 0.9,
          stopLossPrice: isLong ? currentEntryPrice * 0.9 : currentEntryPrice * 1.1,
        };
        const tpslRes = http.patch(
          `${BASE_URL}/api/futures/positions/tpsl`,
          JSON.stringify(tpslPayload),
          businessParams(authHeaders)
        );
        check(tpslRes, {
          "patch tpsl status 200 or 400": (r) => r.status === 200 || r.status === 400,
        });
      }
    }

    sleep(1.0);

    // Action B: Social Position Peek (Try spying on someone from the public leaderboard)
    const boardRes = http.get(`${BASE_URL}/api/futures/leaderboard?limit=5`);
    if (boardRes.status === 200 && boardRes.json("data.entries")) {
      const entries = boardRes.json("data.entries") || [];
      const targets = entries.filter((e) => e.targetToken !== null);

      if (targets.length > 0) {
        const peekTarget = selectRandom(targets);
        const peekPayload = {
          targetToken: peekTarget.targetToken,
          idempotencyKey: `uuid-${randomString(12)}`,
        };

        const peekStartTime = Date.now();
        const peekRes = http.post(
          `${BASE_URL}/api/futures/position-peeks`,
          JSON.stringify(peekPayload),
          businessParams(authHeaders)
        );

        check(peekRes, {
          "peek status 200 or business 400/403": (r) => r.status === 200 || r.status === 400 || r.status === 403,
        });

        peekActionTrend.add(Date.now() - peekStartTime);
      }
    }
  });

  sleep(1);

  // Group 6: Close Position (Clean up to prevent infinite margin depletion)
  group("6. Position Termination", function () {
    const posRes = http.get(`${BASE_URL}/api/futures/positions/me`, paramsWithHeaders(authHeaders));
    if (posRes.status === 200 && Array.isArray(posRes.json("data"))) {
      activePositions = posRes.json("data") || [];
    }

    if (activePositions.length > 0) {
      const position = selectRandom(activePositions);
      sleep(0.5);

      const closePayload = {
        symbol: position.symbol,
        positionSide: position.positionSide,
        marginMode: position.marginMode,
        quantity: position.quantity,
        orderType: "MARKET",
        limitPrice: null,
      };

      const closeStartTime = Date.now();
      const closeRes = http.post(
        `${BASE_URL}/api/futures/positions/close`,
        JSON.stringify(closePayload),
        businessParams(authHeaders)
      );

      const closeOk = check(closeRes, {
        "close position status 200 or business 400": (r) => r.status === 200 || r.status === 400,
        "close success true on 200": (r) => r.status === 400 || r.json("success") === true,
      });

      positionCloseTrend.add(Date.now() - closeStartTime);

      if (!closeOk) {
        failedTransactions.add(1);
      }
    }
  });
}
