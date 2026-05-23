import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
  businessParams,
  canPurchaseShopItem,
  DEFAULT_TEST_PASSWORD,
  extractAccessToken,
  jsonHeaders,
  paramsWithHeaders,
  positiveIntFromEnv,
  precreatedUserForVu,
  setupPrecreatedUsers,
  tiptapParagraph,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

// Custom Performance Metrics
const authDuration = new Trend("auth_duration_ms");
const authDuplicateDuration = new Trend("auth_duplicate_duration_ms");
const authRegisterDuration = new Trend("auth_register_duration_ms");
const authLoginDuration = new Trend("auth_login_duration_ms");
const authOnboardingHttpDuration = new Trend("auth_onboarding_http_duration_ms");
const rewardPurchaseDuration = new Trend("reward_purchase_duration_ms");
const communityActionDuration = new Trend("community_action_duration_ms");
const requestFailures = new Counter("auth_comm_failures");

const AUTH_MEMBER_SCENARIO = (__ENV.AUTH_MEMBER_SCENARIO || "split").toLowerCase();
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");
const AUTH_TARGET_VUS = positiveIntFromEnv("AUTH_TARGET_VUS", 50);
const DEFAULT_STAGES = [
  { duration: "20s", target: stagedTarget(0.2) },
  { duration: "1m", target: stagedTarget(0.6) },
  { duration: "30s", target: AUTH_TARGET_VUS },
  { duration: "30s", target: 0 },
];
const PROBE_STAGES = [
  { duration: "15s", target: stagedTarget(0.2) },
  { duration: "45s", target: stagedTarget(0.6) },
  { duration: "15s", target: AUTH_TARGET_VUS },
  { duration: "15s", target: 0 },
];
const SCENARIO_PRESETS = {
  split: {
    auth_duplicate_probe: {
      executor: "ramping-vus",
      exec: "duplicateAvailabilityProbe",
      stages: PROBE_STAGES,
      gracefulRampDown: "10s",
      tags: { auth_phase: "duplicate" },
    },
    auth_register_probe: {
      executor: "ramping-vus",
      exec: "registerOnlyProbe",
      startTime: "1m40s",
      stages: PROBE_STAGES,
      gracefulRampDown: "10s",
      tags: { auth_phase: "register" },
    },
    auth_login_probe: {
      executor: "ramping-vus",
      exec: "loginOnlyProbe",
      startTime: "3m20s",
      stages: PROBE_STAGES,
      gracefulRampDown: "10s",
      tags: { auth_phase: "login" },
    },
    auth_onboarding_probe: {
      executor: "ramping-vus",
      exec: "onboardingOnlyProbe",
      startTime: "5m",
      stages: PROBE_STAGES,
      gracefulRampDown: "10s",
      tags: { auth_phase: "onboarding" },
    },
  },
  duplicate: {
    auth_duplicate_probe: {
      executor: "ramping-vus",
      exec: "duplicateAvailabilityProbe",
      stages: DEFAULT_STAGES,
      tags: { auth_phase: "duplicate" },
    },
  },
  register: {
    auth_register_probe: {
      executor: "ramping-vus",
      exec: "registerOnlyProbe",
      stages: DEFAULT_STAGES,
      tags: { auth_phase: "register" },
    },
  },
  login: {
    auth_login_probe: {
      executor: "ramping-vus",
      exec: "loginOnlyProbe",
      stages: DEFAULT_STAGES,
      tags: { auth_phase: "login" },
    },
  },
  onboarding: {
    auth_onboarding_probe: {
      executor: "ramping-vus",
      exec: "onboardingOnlyProbe",
      stages: DEFAULT_STAGES,
      tags: { auth_phase: "onboarding" },
    },
  },
  journey: {
    auth_member_journey: {
      executor: "ramping-vus",
      exec: "memberJourney",
      stages: DEFAULT_STAGES,
      tags: { auth_phase: "journey" },
    },
  },
};
const SELECTED_SCENARIOS = SCENARIO_PRESETS[AUTH_MEMBER_SCENARIO] || SCENARIO_PRESETS.split;
const BASE_THRESHOLDS = {
  http_req_failed: ["rate<0.01"],
  http_req_duration: ["p(95)<300"],
  auth_comm_failures: ["count<1"],
};
const AUTH_STEP_THRESHOLDS = {
  auth_duplicate_duration_ms: ["p(95)<150"],
  auth_register_duration_ms: ["p(95)<400"],
  auth_login_duration_ms: ["p(95)<300"],
  auth_onboarding_http_duration_ms: ["p(95)<700"],
};
const THRESHOLD_PRESETS = {
  split: AUTH_STEP_THRESHOLDS,
  duplicate: {
    auth_duplicate_duration_ms: AUTH_STEP_THRESHOLDS.auth_duplicate_duration_ms,
  },
  register: {
    auth_register_duration_ms: AUTH_STEP_THRESHOLDS.auth_register_duration_ms,
  },
  login: {
    auth_login_duration_ms: AUTH_STEP_THRESHOLDS.auth_login_duration_ms,
  },
  onboarding: AUTH_STEP_THRESHOLDS,
  journey: {
    ...AUTH_STEP_THRESHOLDS,
    auth_duration_ms: ["p(95)<400"],
    reward_purchase_duration_ms: ["p(95)<400"],
    community_action_duration_ms: ["p(95)<500"],
  },
};
const SELECTED_THRESHOLDS = THRESHOLD_PRESETS[AUTH_MEMBER_SCENARIO] || THRESHOLD_PRESETS.split;

// k6 Options: split auth probes isolate duplicate/register/login/onboarding bottlenecks.
export const options = {
  scenarios: SELECTED_SCENARIOS,
  thresholds: {
    ...BASE_THRESHOLDS,
    ...SELECTED_THRESHOLDS,
  },
  noCookiesReset: true,
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Helpers for dynamic data generation
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

function stagedTarget(ratio) {
  return Math.max(1, Math.round(AUTH_TARGET_VUS * ratio));
}

function jsonValue(response, path, fallback = null) {
  if (!response || response.body === null || response.body === undefined || response.body === "") {
    return fallback;
  }
  try {
    const value = response.json(path);
    return value === undefined ? fallback : value;
  } catch (_) {
    return fallback;
  }
}

function uniqueAccount(prefix) {
  return `${prefix}_${__VU}_${__ITER}_${randomString(8)}`.slice(0, 60);
}

function buildRegisterPayload(accountId, password = DEFAULT_TEST_PASSWORD) {
  return {
    account: accountId,
    password: password,
    name: `VU User ${randomString(4)}`,
    nickname: `trader_${randomString(6)}`,
    phoneNumber: "010-0000-0000",
    email: `${accountId}@example.com`,
    address: {
      zipcode: "12345",
      address: "Seoul, Antigravity Road",
      addressDetail: "Building B",
    },
  };
}

function authParams(headers, step, extra = {}) {
  return {
    ...extra,
    headers,
    tags: {
      ...(extra.tags || {}),
      auth_step: step,
    },
  };
}

function recordDuration(metric, response) {
  metric.add(response.timings.duration);
  return response.timings.duration;
}

function postDuplicate(accountId, headers) {
  const response = http.post(
    `${BASE_URL}/api/futures/auth/duplicate`,
    JSON.stringify({ account: accountId }),
    authParams(headers, "duplicate")
  );
  recordDuration(authDuplicateDuration, response);
  return response;
}

function postRegister(registerPayload, headers) {
  const response = http.post(
    `${BASE_URL}/api/futures/auth/register`,
    JSON.stringify(registerPayload),
    authParams(headers, "register")
  );
  recordDuration(authRegisterDuration, response);
  return response;
}

function postLogin(accountId, password, headers) {
  const response = http.post(
    `${BASE_URL}/api/futures/auth/login`,
    JSON.stringify({ account: accountId, password: password }),
    authParams(headers, "login")
  );
  recordDuration(authLoginDuration, response);
  return response;
}

export function setup() {
  if (AUTH_MEMBER_SCENARIO === "split" || AUTH_MEMBER_SCENARIO === "login") {
    return setupPrecreatedUsers(BASE_URL, {
      prefix: "k6_auth_login",
      count: positiveIntFromEnv(
        "AUTH_LOGIN_USERS",
        positiveIntFromEnv("PRECREATED_USERS", Math.max(250, AUTH_TARGET_VUS))
      ),
    });
  }
  return {};
}

export default function (data) {
  memberJourney(data);
}

export function duplicateAvailabilityProbe() {
  const headers = jsonHeaders();
  const accountId = uniqueAccount("dup_probe");

  group("auth duplicate availability probe", function () {
    const response = postDuplicate(accountId, headers);
    const ok = check(response, {
      "duplicate probe status 200": (r) => r.status === 200,
      "duplicate probe account available": (r) => jsonValue(r, "data.available") === true,
    });
    if (!ok) {
      requestFailures.add(1);
    }
  });

  sleep(1);
}

export function registerOnlyProbe() {
  const headers = jsonHeaders();
  const accountId = uniqueAccount("reg_probe");

  group("auth register only probe", function () {
    const response = postRegister(buildRegisterPayload(accountId), headers);
    const ok = check(response, {
      "register probe status 200": (r) => r.status === 200,
      "register probe success true": (r) => jsonValue(r, "success") === true,
      "register probe has member id": (r) => jsonValue(r, "data.memberId") !== null,
    });
    if (!ok) {
      requestFailures.add(1);
    }
  });

  sleep(1);
}

export function loginOnlyProbe(data) {
  const headers = jsonHeaders();
  const user = precreatedUserForVu(data);

  group("auth login only probe", function () {
    const response = postLogin(user.account, DEFAULT_TEST_PASSWORD, headers);
    const ok = check(response, {
      "login probe status 200": (r) => r.status === 200,
      "login probe success true": (r) => jsonValue(r, "success") === true,
      "login probe has access token": (r) => extractAccessToken(r) !== null,
    });
    if (!ok) {
      requestFailures.add(1);
    }
  });

  sleep(1);
}

export function onboardingOnlyProbe() {
  const headers = jsonHeaders();
  const accountId = uniqueAccount("onboard_probe");
  const password = DEFAULT_TEST_PASSWORD;

  group("auth onboarding probe", function () {
    const startedAt = Date.now();
    let httpDuration = 0;

    const duplicateRes = postDuplicate(accountId, headers);
    httpDuration += duplicateRes.timings.duration;
    const duplicateOk = check(duplicateRes, {
      "onboarding duplicate status 200": (r) => r.status === 200,
      "onboarding duplicate available": (r) => jsonValue(r, "data.available") === true,
    });
    if (!duplicateOk) {
      requestFailures.add(1);
      return;
    }

    const registerRes = postRegister(buildRegisterPayload(accountId, password), headers);
    httpDuration += registerRes.timings.duration;
    const registerOk = check(registerRes, {
      "onboarding register status 200": (r) => r.status === 200,
      "onboarding register success true": (r) => jsonValue(r, "success") === true,
      "onboarding register has member id": (r) => jsonValue(r, "data.memberId") !== null,
    });
    if (!registerOk) {
      requestFailures.add(1);
      return;
    }

    const loginRes = postLogin(accountId, password, headers);
    httpDuration += loginRes.timings.duration;
    const loginOk = check(loginRes, {
      "onboarding login status 200": (r) => r.status === 200,
      "onboarding login success true": (r) => jsonValue(r, "success") === true,
      "onboarding login has access token": (r) => extractAccessToken(r) !== null,
    });

    authOnboardingHttpDuration.add(httpDuration);
    authDuration.add(Date.now() - startedAt);

    if (!loginOk) {
      requestFailures.add(1);
    }
  });

  sleep(1);
}

export function memberJourney() {
  const accountId = uniqueAccount("vu");
  const password = DEFAULT_TEST_PASSWORD;
  const registerPayload = buildRegisterPayload(accountId, password);

  const headers = jsonHeaders();

  let isLoggedIn = false;
  let accessToken = null;
  let memberId = null;

  // Group 1: Onboarding (Availability -> Register -> Login)
  group("1. Onboarding Flow", function () {
    const startTime = Date.now();
    let httpDuration = 0;

    // 1. Check duplicate
    const duplicateRes = postDuplicate(accountId, headers);
    httpDuration += duplicateRes.timings.duration;
    
    const duplicateCheck = check(duplicateRes, {
      "dup status 200": (r) => r.status === 200,
      "dup success true": (r) => jsonValue(r, "success") === true,
      "account is available": (r) => jsonValue(r, "data.available") === true,
    });

    if (!duplicateCheck) {
      requestFailures.add(1);
      return;
    }

    sleep(0.5);

    // 2. Register
    const registerRes = postRegister(registerPayload, headers);
    httpDuration += registerRes.timings.duration;

    const registerCheck = check(registerRes, {
      "register status 200": (r) => r.status === 200,
      "register success true": (r) => jsonValue(r, "success") === true,
      "has member id": (r) => jsonValue(r, "data.memberId") !== null,
    });

    if (!registerCheck) {
      requestFailures.add(1);
      return;
    }

    memberId = jsonValue(registerRes, "data.memberId");
    sleep(0.5);

    // 3. Login (receives a stateless JWT in the accessToken cookie)
    const loginRes = postLogin(accountId, password, headers);
    httpDuration += loginRes.timings.duration;

    const loginCheck = check(loginRes, {
      "login status 200": (r) => r.status === 200,
      "login success true": (r) => jsonValue(r, "success") === true,
      "has member JWT profile": (r) => jsonValue(r, "data.account") === accountId,
    });

    authDuration.add(Date.now() - startTime);
    authOnboardingHttpDuration.add(httpDuration);

    if (loginCheck) {
      accessToken = extractAccessToken(loginRes);
      isLoggedIn = accessToken !== null;
    } else {
      requestFailures.add(1);
    }
  });

  if (!isLoggedIn) {
    sleep(1);
    return;
  }

  sleep(1);
  const authHeaders = withAccessToken(headers, accessToken);

  // Group 2: Rewards & Shop Interactions
  group("2. Reward & Shop Flow", function () {
    const startTime = Date.now();

    // 1. Fetch current rewards profile
    const rewardsProfileRes = http.get(`${BASE_URL}/api/futures/rewards/me`, paramsWithHeaders(authHeaders));
    check(rewardsProfileRes, {
      "get rewards status 200": (r) => r.status === 200,
      "rewards success true": (r) => jsonValue(r, "success") === true,
      "has valid reward points": (r) => typeof jsonValue(r, "data.rewardPoint") === "number",
    });
    const rewardPoint = rewardsProfileRes.status === 200 ? jsonValue(rewardsProfileRes, "data.rewardPoint", 0) || 0 : 0;

    // 2. Fetch point logs
    const historyRes = http.get(`${BASE_URL}/api/futures/rewards/history`, paramsWithHeaders(authHeaders));
    check(historyRes, {
      "get points history status 200": (r) => r.status === 200,
    });

    // 3. Fetch store catalog
    const shopRes = http.get(`${BASE_URL}/api/futures/shop/items`, paramsWithHeaders(authHeaders));
    const shopCheck = check(shopRes, {
      "get shop status 200": (r) => r.status === 200,
      "has shop items": (r) => Array.isArray(jsonValue(r, "data")),
    });

    if (shopCheck) {
      const items = jsonValue(shopRes, "data", []) || [];
      const buyableItems = items.filter((item) => canPurchaseShopItem(item, rewardPoint));
      
      if (buyableItems.length > 0) {
        const itemToBuy = selectRandom(buyableItems);
        
        sleep(0.5);

        // 4. Try purchasing the virtual item (e.g. REFILL or PEEK)
        const purchaseRes = http.post(
          `${BASE_URL}/api/futures/shop/items/${itemToBuy.code}/purchase`,
          null,
          businessParams(authHeaders)
        );

        check(purchaseRes, {
          "purchase status 200 or business 400": (r) => r.status === 200 || r.status === 400,
        });
      }
    }

    rewardPurchaseDuration.add(Date.now() - startTime);
  });

  sleep(1);

  // Group 3: Community Engagements
  group("3. Community Engagement Flow", function () {
    const startTime = Date.now();

    // 1. Browse community post categories (e.g., CHART_ANALYSIS or CHAT)
    const category = selectRandom(["CHART_ANALYSIS", "CHAT", "COIN_INFORMATION"]);
    const postsRes = http.get(
      `${BASE_URL}/api/futures/community/posts?category=${category}&page=0&size=10`,
      paramsWithHeaders(authHeaders)
    );
    
    const postsCheck = check(postsRes, {
      "posts status 200": (r) => r.status === 200,
      "posts array returned": (r) => Array.isArray(jsonValue(r, "data.posts")),
    });

    let targetPostId = null;

    if (postsCheck) {
      const posts = jsonValue(postsRes, "data.posts", []) || [];
      if (posts.length > 0) {
        targetPostId = selectRandom(posts).id;
      }
    }

    sleep(1);

    // 2. Create a new community post
    const postPayload = {
      category: category,
      title: `Load Testing Analysis: ${randomString(10)}`,
      contentJson: tiptapParagraph(
        `This post was generated dynamically by k6 VU #${__VU}. Long live standard web specifications!`
      ),
      imageObjectKeys: [],
    };

    const createPostRes = http.post(
      `${BASE_URL}/api/futures/community/posts`,
      JSON.stringify(postPayload),
      { headers: authHeaders }
    );

    const postCreateCheck = check(createPostRes, {
      "create post status 200": (r) => r.status === 200,
      "create post success true": (r) => jsonValue(r, "success") === true,
      "created post id exists": (r) => jsonValue(r, "data.postId") !== null,
    });

    if (postCreateCheck) {
      targetPostId = jsonValue(createPostRes, "data.postId");
    } else {
      requestFailures.add(1);
    }

    if (targetPostId) {
      sleep(0.5);

      // 3. View detail of the post (increments view count)
      const detailRes = http.get(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}`,
        paramsWithHeaders(authHeaders)
      );
      check(detailRes, {
        "view post detail status 200": (r) => r.status === 200,
      });

      sleep(0.5);

      // 4. Like/Unlike post
      const likeRes = http.post(`${BASE_URL}/api/futures/community/posts/${targetPostId}/like`, null, { headers: authHeaders });
      check(likeRes, {
        "like post status 200": (r) => r.status === 200,
      });

      sleep(0.5);

      // 5. Read comments on the post
      const commentsRes = http.get(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}/comments?page=0&size=10`,
        paramsWithHeaders(authHeaders)
      );
      check(commentsRes, {
        "get comments status 200": (r) => r.status === 200,
      });

      sleep(0.5);

      // 6. Write a comment on the post
      const commentPayload = {
        content: `Very impressive load testing post. VU #${__VU} approves!`,
      };
      const writeCommentRes = http.post(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}/comments`,
        JSON.stringify(commentPayload),
        { headers: authHeaders }
      );
      check(writeCommentRes, {
        "write comment status 200": (r) => r.status === 200,
        "comment creation success": (r) => jsonValue(r, "success") === true,
      });
    }

    communityActionDuration.add(Date.now() - startTime);
  });

  sleep(1);

  // Group 4: Logout Cleanup
  group("4. Logout Flow", function () {
    const logoutRes = http.post(`${BASE_URL}/api/futures/auth/logout`, null, { headers: authHeaders });
    check(logoutRes, {
      "logout status 200": (r) => r.status === 200,
    });
  });
}
