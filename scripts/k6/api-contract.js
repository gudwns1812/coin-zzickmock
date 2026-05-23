import http from "k6/http";

export const SUPPORTED_MARKET_SYMBOLS = ["BTCUSDT", "ETHUSDT"];
export const DEFAULT_ORIGIN = __ENV.ORIGIN || "http://localhost:3000";
export const DEFAULT_TEST_PASSWORD = __ENV.TEST_USER_PASSWORD || "Password123!";

export const BUSINESS_STATUS_CALLBACK = http.expectedStatuses(
  { min: 200, max: 399 },
  400,
  403
);

export function supportedSymbolsFromEnv(envName = "SYMBOLS") {
  const rawSymbols = (__ENV[envName] || SUPPORTED_MARKET_SYMBOLS.join(","))
    .split(",")
    .map((symbol) => symbol.trim().toUpperCase())
    .filter((symbol) => SUPPORTED_MARKET_SYMBOLS.includes(symbol));

  return rawSymbols.length > 0 ? rawSymbols : SUPPORTED_MARKET_SYMBOLS;
}

export function jsonHeaders(extra = {}) {
  return {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "Origin": DEFAULT_ORIGIN,
    ...extra,
  };
}

export function eventStreamHeaders(extra = {}) {
  return {
    "Accept": "text/event-stream",
    ...extra,
  };
}

export function withAccessToken(headers, accessToken) {
  return {
    ...headers,
    "Cookie": `accessToken=${accessToken}`,
  };
}

export function extractAccessToken(response) {
  const setCookie = response.headers["Set-Cookie"] || response.headers["set-cookie"] || "";
  const match = String(setCookie).match(/(?:^|[,;\s])accessToken=([^;,\s]+)/);
  return match && match[1] ? match[1] : null;
}

export function positiveIntFromEnv(envName, fallback) {
  const parsed = Number.parseInt(__ENV[envName], 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function precreatedUserCount(fallback = 250) {
  return positiveIntFromEnv("PRECREATED_USERS", fallback);
}

export function precreatedAccountPrefix(defaultPrefix) {
  return (__ENV.TEST_ACCOUNT_PREFIX || defaultPrefix).replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 48);
}

export function buildPrecreatedCredential(prefix, index, password = DEFAULT_TEST_PASSWORD) {
  const padded = String(index).padStart(5, "0");
  const account = `${prefix}_${padded}`;
  return {
    account,
    password,
    payload: {
      account,
      password,
      name: `K6 User ${padded}`,
      nickname: `${prefix}_${padded}`.slice(0, 30),
      phoneNumber: `010-${String(7000 + (index % 1000)).padStart(4, "0")}-${String(index % 10000).padStart(4, "0")}`,
      email: `${account}@k6.local`,
      address: {
        zipcode: "12345",
        address: "Seoul, K6 Load Street",
        addressDetail: `Suite ${padded}`,
      },
    },
  };
}

export function ensurePrecreatedCredential(baseUrl, credential, headers = jsonHeaders()) {
  http.post(
    `${baseUrl}/api/futures/auth/register`,
    JSON.stringify(credential.payload),
    {
      headers,
      responseCallback: http.expectedStatuses(200, 400, 409),
    }
  );

  const loginRes = http.post(
    `${baseUrl}/api/futures/auth/login`,
    JSON.stringify({ account: credential.account, password: credential.password }),
    { headers }
  );
  const accessToken = loginRes.status === 200 ? extractAccessToken(loginRes) : null;

  if (!accessToken) {
    throw new Error(`Failed to pre-authenticate k6 account ${credential.account}: status=${loginRes.status}`);
  }

  return {
    account: credential.account,
    accessToken,
  };
}

export function setupPrecreatedUsers(baseUrl, options = {}) {
  const prefix = precreatedAccountPrefix(options.prefix || "k6_user");
  const count = options.count || precreatedUserCount(250);
  const password = options.password || DEFAULT_TEST_PASSWORD;
  const headers = jsonHeaders();
  const users = [];

  console.log(`[setup] ensuring ${count} precreated k6 accounts with prefix=${prefix}`);
  for (let index = 1; index <= count; index++) {
    const credential = buildPrecreatedCredential(prefix, index, password);
    users.push(ensurePrecreatedCredential(baseUrl, credential, headers));
  }
  console.log(`[setup] ready ${users.length} pre-authenticated k6 accounts`);

  return {
    precreatedUsers: users,
  };
}

export function precreatedUserForVu(data) {
  const users = data && Array.isArray(data.precreatedUsers) ? data.precreatedUsers : [];
  if (users.length === 0) {
    throw new Error("No precreatedUsers were provided by setup()");
  }
  return users[(__VU - 1) % users.length];
}

export function paramsWithHeaders(headers, extra = {}) {
  return {
    ...extra,
    headers,
  };
}

export function businessParams(headers, extra = {}) {
  return {
    ...extra,
    headers,
    responseCallback: BUSINESS_STATUS_CALLBACK,
  };
}

export function tiptapParagraph(text) {
  return {
    type: "doc",
    content: [
      {
        type: "paragraph",
        content: [
          {
            type: "text",
            text,
          },
        ],
      },
    ],
  };
}

export function canPurchaseShopItem(item, rewardPoint) {
  if (
    !item ||
    !["ACCOUNT_REFILL_COUNT", "POSITION_PEEK"].includes(item.itemType) ||
    !item.active ||
    typeof item.price !== "number" ||
    item.price > rewardPoint
  ) {
    return false;
  }
  if (typeof item.remainingStock === "number" && item.remainingStock <= 0) {
    return false;
  }
  if (typeof item.remainingPurchaseLimit === "number" && item.remainingPurchaseLimit <= 0) {
    return false;
  }
  return true;
}
