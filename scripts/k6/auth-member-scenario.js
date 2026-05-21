import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";

// Custom Performance Metrics
const authDuration = new Trend("auth_duration_ms");
const rewardPurchaseDuration = new Trend("reward_purchase_duration_ms");
const communityActionDuration = new Trend("community_action_duration_ms");
const requestFailures = new Counter("auth_comm_failures");

// k6 Options: Ramping up to 50 concurrent VUs representing social/auth users
export const options = {
  stages: [
    { duration: "20s", target: 10 },  // Warm-up: 0 to 10 VUs
    { duration: "1m", target: 30 },   // Normal load: up to 30 VUs
    { duration: "30s", target: 50 },  // Peak spike: up to 50 VUs
    { duration: "30s", target: 0 },   // Cool-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],                  // Request failure rate < 1%
    http_req_duration: ["p(95)<300"],                // 95% of requests under 300ms
    auth_duration_ms: ["p(95)<400"],
    reward_purchase_duration_ms: ["p(95)<400"],
    community_action_duration_ms: ["p(95)<500"],
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

// Base configurations
const BASE_URL = (__ENV.BASE_URL || "http://localhost:18080").replace(/\/$/, "");

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

export default function () {
  const accountId = `vu_${randomString(8)}`;
  const password = "Password123!";
  const name = `VU User ${randomString(4)}`;
  const nickname = `trader_${randomString(6)}`;
  const email = `${accountId}@example.com`;

  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
  };

  let isLoggedIn = false;
  let memberId = null;

  // Group 1: Onboarding (Availability -> Register -> Login)
  group("1. Onboarding Flow", function () {
    const startTime = Date.now();

    // 1. Check duplicate
    const duplicateRes = http.post(
      `${BASE_URL}/api/futures/auth/duplicate`,
      JSON.stringify({ account: accountId }),
      { headers }
    );
    
    const duplicateCheck = check(duplicateRes, {
      "dup status 200": (r) => r.status === 200,
      "dup success true": (r) => r.json("success") === true,
      "account is available": (r) => r.json("data.available") === true,
    });

    if (!duplicateCheck) {
      requestFailures.add(1);
      return;
    }

    sleep(0.5);

    // 2. Register
    const registerPayload = {
      account: accountId,
      password: password,
      name: name,
      nickname: nickname,
      phoneNumber: "010-0000-0000",
      email: email,
      address: {
        zipcode: "12345",
        address: "Seoul, Antigravity Road",
        addressDetail: "Building B",
      },
    };

    const registerRes = http.post(
      `${BASE_URL}/api/futures/auth/register`,
      JSON.stringify(registerPayload),
      { headers }
    );

    const registerCheck = check(registerRes, {
      "register status 200": (r) => r.status === 200,
      "register success true": (r) => r.json("success") === true,
      "has member id": (r) => r.json("data.memberId") !== null,
    });

    if (!registerCheck) {
      requestFailures.add(1);
      return;
    }

    memberId = registerRes.json("data.memberId");
    sleep(0.5);

    // 3. Login (sets the 'accessToken' cookie in k6's default cookie jar)
    const loginPayload = {
      account: accountId,
      password: password,
    };

    const loginRes = http.post(
      `${BASE_URL}/api/futures/auth/login`,
      JSON.stringify(loginPayload),
      { headers }
    );

    const loginCheck = check(loginRes, {
      "login status 200": (r) => r.status === 200,
      "login success true": (r) => r.json("success") === true,
      "has member session": (r) => r.json("data.account") === accountId,
    });

    authDuration.add(Date.now() - startTime);

    if (loginCheck) {
      isLoggedIn = true;
    } else {
      requestFailures.add(1);
    }
  });

  if (!isLoggedIn) {
    sleep(1);
    return;
  }

  sleep(1);

  // Group 2: Rewards & Shop Interactions
  group("2. Reward & Shop Flow", function () {
    const startTime = Date.now();

    // 1. Fetch current rewards profile
    const rewardsProfileRes = http.get(`${BASE_URL}/api/futures/rewards/me`);
    check(rewardsProfileRes, {
      "get rewards status 200": (r) => r.status === 200,
      "rewards success true": (r) => r.json("success") === true,
      "has valid points": (r) => typeof r.json("data.points") === "number",
    });

    // 2. Fetch point logs
    const historyRes = http.get(`${BASE_URL}/api/futures/rewards/history`);
    check(historyRes, {
      "get points history status 200": (r) => r.status === 200,
    });

    // 3. Fetch store catalog
    const shopRes = http.get(`${BASE_URL}/api/futures/shop/items`);
    const shopCheck = check(shopRes, {
      "get shop status 200": (r) => r.status === 200,
      "has shop items": (r) => Array.isArray(r.json("data")),
    });

    if (shopCheck) {
      const items = shopRes.json("data") || [];
      const buyableItems = items.filter((item) => item.active && item.price <= 50000000000); // just choose any active
      
      if (buyableItems.length > 0) {
        const itemToBuy = selectRandom(buyableItems);
        
        sleep(0.5);

        // 4. Try purchasing the virtual item (e.g. REFILL or PEEK)
        const purchaseRes = http.post(
          `${BASE_URL}/api/futures/shop/items/${itemToBuy.code}/purchase`,
          null,
          { headers }
        );

        check(purchaseRes, {
          "purchase status 200 or 400": (r) => r.status === 200 || r.status === 400, // 400 means insufficient points, which is expected
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
    const postsRes = http.get(`${BASE_URL}/api/futures/community/posts?category=${category}&page=0&size=10`);
    
    const postsCheck = check(postsRes, {
      "posts status 200": (r) => r.status === 200,
      "posts array returned": (r) => Array.isArray(r.json("data.posts")),
    });

    let targetPostId = null;

    if (postsCheck) {
      const posts = postsRes.json("data.posts") || [];
      if (posts.length > 0) {
        targetPostId = selectRandom(posts).id;
      }
    }

    sleep(1);

    // 2. Create a new community post
    const postPayload = {
      category: category,
      title: `Load Testing Analysis: ${randomString(10)}`,
      contentJson: {
        type: "doc",
        content: [
          {
            type: "paragraph",
            text: `This post was generated dynamically by k6 VU #${__VU}. Long live standard web specifications!`,
          },
        ],
      },
      imageObjectKeys: [],
    };

    const createPostRes = http.post(
      `${BASE_URL}/api/futures/community/posts`,
      JSON.stringify(postPayload),
      { headers }
    );

    const postCreateCheck = check(createPostRes, {
      "create post status 200": (r) => r.status === 200,
      "create post success true": (r) => r.json("success") === true,
      "created post id exists": (r) => r.json("data.postId") !== null,
    });

    if (postCreateCheck) {
      targetPostId = createPostRes.json("data.postId");
    }

    if (targetPostId) {
      sleep(0.5);

      // 3. View detail of the post (increments view count)
      const detailRes = http.get(`${BASE_URL}/api/futures/community/posts/${targetPostId}`);
      check(detailRes, {
        "view post detail status 200": (r) => r.status === 200,
      });

      sleep(0.5);

      // 4. Like/Unlike post
      const likeRes = http.post(`${BASE_URL}/api/futures/community/posts/${targetPostId}/like`, null, { headers });
      check(likeRes, {
        "like post status 200": (r) => r.status === 200,
      });

      sleep(0.5);

      // 5. Read comments on the post
      const commentsRes = http.get(`${BASE_URL}/api/futures/community/posts/${targetPostId}/comments?page=0&size=10`);
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
        { headers }
      );
      check(writeCommentRes, {
        "write comment status 200": (r) => r.status === 200,
        "comment creation success": (r) => r.json("success") === true,
      });
    }

    communityActionDuration.add(Date.now() - startTime);
  });

  sleep(1);

  // Group 4: Logout Cleanup
  group("4. Logout Flow", function () {
    const logoutRes = http.post(`${BASE_URL}/api/futures/auth/logout`, null, { headers });
    check(logoutRes, {
      "logout status 200": (r) => r.status === 200,
    });
  });
}
