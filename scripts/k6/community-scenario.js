import http from "k6/http";
import { check, group, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
  jsonHeaders,
  paramsWithHeaders,
  positiveIntFromEnv,
  precreatedUserCount,
  precreatedUserForVu,
  setupPrecreatedUsers,
  tiptapParagraph,
  withAccessToken,
} from "./api-contract.js";
export { handleSummary } from "./k6-report.mjs";

const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");
const COMMUNITY_SCENARIO = (__ENV.COMMUNITY_SCENARIO || "reaction").toLowerCase();
const COMMUNITY_TARGET_VUS = positiveIntFromEnv("COMMUNITY_TARGET_VUS", 500);
const COMMUNITY_TEST_DURATION = __ENV.COMMUNITY_TEST_DURATION || "5m";
const COMMUNITY_RAMP_UP = __ENV.COMMUNITY_RAMP_UP || "5m";
const COMMUNITY_RAMP_DOWN = __ENV.COMMUNITY_RAMP_DOWN || "2m";
const COMMUNITY_PAGE_SIZE = positiveIntFromEnv("COMMUNITY_PAGE_SIZE", 20);
const COMMUNITY_COMMENT_READ_SIZE = positiveIntFromEnv("COMMUNITY_COMMENT_READ_SIZE", 10);
const COMMUNITY_MAX_BROWSE_PAGE = positiveIntFromEnv("COMMUNITY_MAX_BROWSE_PAGE", 2);
const COMMUNITY_START_JITTER_MS = positiveIntFromEnv("COMMUNITY_START_JITTER_MS", 30000);
const COMMUNITY_THINK_MIN_MS = positiveIntFromEnv("COMMUNITY_THINK_MIN_MS", 750);
const COMMUNITY_THINK_MAX_MS = positiveIntFromEnv("COMMUNITY_THINK_MAX_MS", 3000);
const COMMUNITY_LOOP_JITTER_MIN_MS = positiveIntFromEnv("COMMUNITY_LOOP_JITTER_MIN_MS", 1500);
const COMMUNITY_LOOP_JITTER_MAX_MS = positiveIntFromEnv("COMMUNITY_LOOP_JITTER_MAX_MS", 7000);
const COMMUNITY_SEED_POSTS_PER_CATEGORY = positiveIntFromEnv("COMMUNITY_SEED_POSTS_PER_CATEGORY", 45);
const COMMUNITY_FAILURE_LOG_LIMIT_PER_VU = positiveIntFromEnv("COMMUNITY_FAILURE_LOG_LIMIT_PER_VU", 3);
const COMMUNITY_CREATE_RATE_MULTIPLIER = probabilityMultiplierFromEnv("COMMUNITY_CREATE_RATE_MULTIPLIER", 1);
const COMMUNITY_LIKE_RATE_MULTIPLIER = probabilityMultiplierFromEnv("COMMUNITY_LIKE_RATE_MULTIPLIER", 1);
const COMMUNITY_COMMENT_RATE_MULTIPLIER = probabilityMultiplierFromEnv("COMMUNITY_COMMENT_RATE_MULTIPLIER", 1);
const ALLOW_SHARED_COMMUNITY_USERS = (__ENV.ALLOW_SHARED_COMMUNITY_USERS || "false").toLowerCase() === "true";
const CATEGORIES = ["CHART_ANALYSIS", "COIN_INFORMATION", "CHAT"];
const PROFILE_PRESETS = {
  reaction: [
    {
      name: "comment_writer",
      share: 40,
      viewRate: 0.95,
      commentReadRate: 0.85,
      createRate: 0.01,
      likeRate: 0.75,
      commentRate: 0.55,
    },
    {
      name: "like_reactor",
      share: 40,
      viewRate: 0.90,
      commentReadRate: 0.70,
      createRate: 0.005,
      likeRate: 0.85,
      commentRate: 0.25,
    },
    {
      name: "reader_reactor",
      share: 20,
      viewRate: 0.80,
      commentReadRate: 0.45,
      createRate: 0,
      likeRate: 0.35,
      commentRate: 0.10,
    },
  ],
  mixed: [
    {
      name: "writer",
      share: 5,
      viewRate: 0.95,
      commentReadRate: 0.85,
      createRate: 0.35,
      likeRate: 0.30,
      commentRate: 0.20,
    },
    {
      name: "commenter",
      share: 15,
      viewRate: 0.90,
      commentReadRate: 0.80,
      createRate: 0.05,
      likeRate: 0.20,
      commentRate: 0.25,
    },
    {
      name: "reactor",
      share: 30,
      viewRate: 0.80,
      commentReadRate: 0.60,
      createRate: 0.01,
      likeRate: 0.35,
      commentRate: 0.05,
    },
    {
      name: "reader",
      share: 50,
      viewRate: 0.65,
      commentReadRate: 0.35,
      createRate: 0.005,
      likeRate: 0.05,
      commentRate: 0.005,
    },
  ],
};
const USER_PROFILES = PROFILE_PRESETS[COMMUNITY_SCENARIO] || PROFILE_PRESETS.reaction;
const STATIC_USERS = loadStaticUsers();

const communityCreatePostDuration = new Trend("community_create_post_ms");
const communityListPostsDuration = new Trend("community_list_posts_ms");
const communityViewPostDuration = new Trend("community_view_post_ms");
const communityLikeDuration = new Trend("community_like_ms");
const communityCommentReadDuration = new Trend("community_comment_read_ms");
const communityCommentWriteDuration = new Trend("community_comment_write_ms");
const communityScenarioDuration = new Trend("community_scenario_iteration_ms");
const communityLikeAttempts = new Counter("community_like_attempts");
const communityLikeSuccesses = new Counter("community_like_successes");
const communityCommentWriteAttempts = new Counter("community_comment_write_attempts");
const communityCommentWriteSuccesses = new Counter("community_comment_write_successes");
const communityFailures = new Counter("community_scenario_failures");

export const options = {
  setupTimeout: "8m",
  stages: [
    { duration: COMMUNITY_RAMP_UP, target: COMMUNITY_TARGET_VUS },
    { duration: COMMUNITY_TEST_DURATION, target: COMMUNITY_TARGET_VUS },
    { duration: COMMUNITY_RAMP_DOWN, target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<800"],
    community_scenario_failures: ["count<1"],
    community_create_post_ms: ["p(95)<1000"],
    community_list_posts_ms: ["p(95)<500"],
    community_view_post_ms: ["p(95)<700"],
    community_like_ms: ["p(95)<700"],
    community_comment_read_ms: ["p(95)<500"],
    community_comment_write_ms: ["p(95)<800"],
    community_scenario_iteration_ms: ["p(95)<20000"],
  },
  noCookiesReset: true,
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

let vuAccessToken = null;
let vuFailureLogs = 0;
let initialJitterApplied = false;

export function setup() {
  if (STATIC_USERS.length > 0) {
    console.log(`[setup] using ${STATIC_USERS.length} pre-authenticated community users from COMMUNITY_USERS_FILE`);
    validatePrecreatedUserPool(STATIC_USERS);
    const seededPostIdsByCategory = seedCommunityPosts(STATIC_USERS);
    return {
      precreatedUsers: STATIC_USERS,
      seededPostIdsByCategory,
    };
  }

  const setupData = setupPrecreatedUsers(BASE_URL, {
    prefix: "k6_community",
    count: Math.max(precreatedUserCount(COMMUNITY_TARGET_VUS), COMMUNITY_TARGET_VUS),
  });
  validatePrecreatedUserPool(setupData.precreatedUsers || []);
  return {
    ...setupData,
    seededPostIdsByCategory: seedCommunityPosts(setupData.precreatedUsers || []),
  };
}

export default function (data) {
  if (!vuAccessToken) {
    vuAccessToken = precreatedUserForVu(data).accessToken;
  }
  applyInitialJitter();
  const iterationStart = Date.now();

  const authHeaders = withAccessToken(jsonHeaders(), vuAccessToken);
  const category = selectRandom(CATEGORIES);
  const profile = profileForVu(__VU);
  let createdPostId = null;
  let targetPostId = null;

  if (shouldRun(scaledRate(profile.createRate, COMMUNITY_CREATE_RATE_MULTIPLIER))) {
    group("1. write community post", function () {
      const title = `k6 distributed community post ${__VU}-${__ITER}-${randomString(8)}`;
      const payload = {
        category,
        title,
        contentJson: tiptapParagraph(
          `커뮤니티 분산 부하테스트 글입니다. vu=${__VU}, iter=${__ITER}, category=${category}, profile=${profile.name}`
        ),
        imageObjectKeys: [],
      };
      const response = http.post(
        `${BASE_URL}/api/futures/community/posts`,
        JSON.stringify(payload),
        { headers: authHeaders, tags: { community_step: "create_post", community_profile: profile.name } }
      );
      communityCreatePostDuration.add(response.timings.duration);
      const ok = check(response, {
        "create post returns 200": (r) => r.status === 200,
        "create post response success": (r) => jsonValue(r, "success") === true,
        "create post id exists": (r) => jsonValue(r, "data.postId") !== null,
      });
      if (ok) {
        createdPostId = jsonValue(response, "data.postId");
        targetPostId = createdPostId;
      } else {
        recordFailure("create_post", response, { category, profile: profile.name });
      }
    });

    think();
  }

  think();

  group("2. browse community list", function () {
    const browsePage = randomInt(0, COMMUNITY_MAX_BROWSE_PAGE);
    const response = http.get(
      `${BASE_URL}/api/futures/community/posts?category=${category}&page=${browsePage}&size=${COMMUNITY_PAGE_SIZE}`,
      paramsWithHeaders(authHeaders, { tags: { community_step: "list_posts", community_profile: profile.name } })
    );
    communityListPostsDuration.add(response.timings.duration);
    const ok = check(response, {
      "list posts returns 200": (r) => r.status === 200,
      "list posts array exists": (r) => Array.isArray(jsonValue(r, "data.posts")),
    });
    if (ok) {
      const posts = jsonValue(response, "data.posts", []) || [];
      if (posts.length > 0) {
        targetPostId = selectRandom(posts).id || targetPostId;
      } else {
        targetPostId = selectSeededPostId(data, category) || targetPostId;
      }
    } else {
      recordFailure("list_posts", response, { category, browsePage, profile: profile.name });
    }
  });

  think();

  if (targetPostId && shouldRun(profile.viewRate)) {
    group("3. view post detail", function () {
      const response = http.get(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}`,
        paramsWithHeaders(authHeaders, { tags: { community_step: "view_post", community_profile: profile.name } })
      );
      communityViewPostDuration.add(response.timings.duration);
      const ok = check(response, {
        "view post returns 200": (r) => r.status === 200,
        "view post id matches": (r) => jsonValue(r, "data.id") === targetPostId,
      });
      if (!ok) {
        recordFailure("view_post", response, { targetPostId, profile: profile.name });
      }
    });

    think();
  }

  if (targetPostId && shouldRun(scaledRate(profile.likeRate, COMMUNITY_LIKE_RATE_MULTIPLIER))) {
    group("4. like post", function () {
      communityLikeAttempts.add(1);
      const response = http.post(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}/like`,
        null,
        { headers: authHeaders, tags: { community_step: "like_post", community_profile: profile.name } }
      );
      communityLikeDuration.add(response.timings.duration);
      const ok = check(response, {
        "like post returns 200": (r) => r.status === 200,
        "like post likedByMe true": (r) => jsonValue(r, "data.likedByMe") === true,
      });
      if (!ok) {
        recordFailure("like_post", response, { targetPostId, profile: profile.name });
      } else {
        communityLikeSuccesses.add(1);
      }
    });

    think();
  }

  if (targetPostId && shouldRun(profile.commentReadRate)) {
    group("5. read comments", function () {
      const response = http.get(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}/comments?page=0&size=${COMMUNITY_COMMENT_READ_SIZE}`,
        paramsWithHeaders(authHeaders, { tags: { community_step: "read_comments", community_profile: profile.name } })
      );
      communityCommentReadDuration.add(response.timings.duration);
      const ok = check(response, {
        "read comments returns 200": (r) => r.status === 200,
        "read comments array exists": (r) => Array.isArray(jsonValue(r, "data.comments")),
      });
      if (!ok) {
        recordFailure("read_comments", response, { targetPostId, profile: profile.name });
      }
    });

    think();
  }

  if (targetPostId && shouldRun(scaledRate(profile.commentRate, COMMUNITY_COMMENT_RATE_MULTIPLIER))) {
    group("6. write comment", function () {
      communityCommentWriteAttempts.add(1);
      const payload = {
        content: `분산 댓글 부하테스트입니다. vu=${__VU}, iter=${__ITER}, post=${targetPostId}, profile=${profile.name}`,
      };
      const response = http.post(
        `${BASE_URL}/api/futures/community/posts/${targetPostId}/comments`,
        JSON.stringify(payload),
        { headers: authHeaders, tags: { community_step: "write_comment", community_profile: profile.name } }
      );
      communityCommentWriteDuration.add(response.timings.duration);
      const ok = check(response, {
        "write comment returns 200": (r) => r.status === 200,
        "write comment id exists": (r) => jsonValue(r, "data.commentId") !== null,
      });
      if (!ok) {
        recordFailure("write_comment", response, { targetPostId, profile: profile.name });
      } else {
        communityCommentWriteSuccesses.add(1);
      }
    });
  }

  if (!targetPostId && !createdPostId) {
    communityFailures.add(1);
    logFailure({
      event: "community_scenario.no_target_post",
      vu: __VU,
      iter: __ITER,
      createdPostId,
      category,
      profile: profile.name,
    });
  }

  communityScenarioDuration.add(Date.now() - iterationStart);
  loopJitter();
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

function randomInt(min, max) {
  return Math.floor(randomRange(min, max + 1));
}

function shouldRun(rate) {
  return Math.random() < Math.min(1, Math.max(0, rate));
}

function think() {
  sleep(randomRange(COMMUNITY_THINK_MIN_MS, COMMUNITY_THINK_MAX_MS) / 1000);
}

function loopJitter() {
  sleep(randomRange(COMMUNITY_LOOP_JITTER_MIN_MS, COMMUNITY_LOOP_JITTER_MAX_MS) / 1000);
}

function applyInitialJitter() {
  if (initialJitterApplied || COMMUNITY_START_JITTER_MS <= 0) {
    return;
  }
  initialJitterApplied = true;
  sleep(randomRange(0, COMMUNITY_START_JITTER_MS) / 1000);
}

function profileForVu(vu) {
  const bucket = ((vu - 1) % 100) + 1;
  let upperBound = 0;
  for (const profile of USER_PROFILES) {
    upperBound += profile.share;
    if (bucket <= upperBound) {
      return profile;
    }
  }
  return USER_PROFILES[USER_PROFILES.length - 1];
}

function validatePrecreatedUserPool(users) {
  if (ALLOW_SHARED_COMMUNITY_USERS || users.length >= COMMUNITY_TARGET_VUS) {
    return;
  }
  throw new Error(
    `COMMUNITY_TARGET_VUS=${COMMUNITY_TARGET_VUS} requires at least ${COMMUNITY_TARGET_VUS} precreated users. ` +
    `Got ${users.length}. Run npm run k6:community:prepare-users with COMMUNITY_TARGET_VUS=${COMMUNITY_TARGET_VUS}, ` +
    "or set ALLOW_SHARED_COMMUNITY_USERS=true for an intentional shared-account run."
  );
}

function scaledRate(rate, multiplier) {
  return Math.min(1, Math.max(0, rate * multiplier));
}

function probabilityMultiplierFromEnv(envName, fallback) {
  const parsed = Number.parseFloat(__ENV[envName]);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return fallback;
  }
  return parsed;
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

function recordFailure(operation, response, context = {}) {
  communityFailures.add(1);
  const body = response && response.body !== null && response.body !== undefined
    ? String(response.body).replace(/\s+/g, " ").slice(0, 500)
    : "";
  logFailure({
    event: "community_scenario.unexpected_response",
    operation,
    status: response ? response.status : null,
    vu: __VU,
    iter: __ITER,
    context,
    body,
  });
}

function logFailure(event) {
  if (vuFailureLogs >= COMMUNITY_FAILURE_LOG_LIMIT_PER_VU) {
    return;
  }
  vuFailureLogs += 1;
  console.log(JSON.stringify(event));
}

function loadStaticUsers() {
  const filePath = __ENV.COMMUNITY_USERS_FILE;
  if (!filePath) {
    return [];
  }
  try {
    const parsed = JSON.parse(open(filePath));
    if (Array.isArray(parsed)) {
      return parsed;
    }
    if (parsed && Array.isArray(parsed.precreatedUsers)) {
      return parsed.precreatedUsers;
    }
  } catch (error) {
    throw new Error(`Failed to load COMMUNITY_USERS_FILE=${filePath}: ${error.message}`);
  }
  throw new Error(`COMMUNITY_USERS_FILE=${filePath} must contain an array or { "precreatedUsers": [] }`);
}

function seedCommunityPosts(users) {
  const seededPostIdsByCategory = {};
  for (const category of CATEGORIES) {
    seededPostIdsByCategory[category] = [];
  }
  if (COMMUNITY_SEED_POSTS_PER_CATEGORY <= 0 || users.length === 0) {
    return seededPostIdsByCategory;
  }

  for (const category of CATEGORIES) {
    for (let index = 0; index < COMMUNITY_SEED_POSTS_PER_CATEGORY; index += 1) {
      const user = users[(index + category.length) % users.length];
      const response = http.post(
        `${BASE_URL}/api/futures/community/posts`,
        JSON.stringify({
          category,
          title: `k6 seed ${category} ${index + 1} ${randomString(8)}`,
          contentJson: tiptapParagraph(`커뮤니티 500VU 분산 타깃 seed post입니다. category=${category}, seed=${index + 1}`),
          imageObjectKeys: [],
        }),
        {
          headers: withAccessToken(jsonHeaders(), user.accessToken),
          tags: { community_step: "seed_post", community_profile: "setup" },
        }
      );
      const postId = jsonValue(response, "data.postId");
      if (response.status === 200 && postId !== null) {
        seededPostIdsByCategory[category].push(postId);
      }
    }
  }
  console.log(`[setup] seeded community posts: ${JSON.stringify(seededPostIdsByCategory)}`);
  return seededPostIdsByCategory;
}

function selectSeededPostId(data, category) {
  const seeded = data && data.seededPostIdsByCategory && data.seededPostIdsByCategory[category];
  if (!Array.isArray(seeded) || seeded.length === 0) {
    return null;
  }
  return selectRandom(seeded);
}
