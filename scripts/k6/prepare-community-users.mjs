#!/usr/bin/env node
import { createHmac } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import mysql from "mysql2/promise";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "../..");

const count = positiveInt(process.env.COMMUNITY_TARGET_VUS || process.env.PRECREATED_USERS, 500);
const prefix = sanitizePrefix(process.env.TEST_ACCOUNT_PREFIX || `k6_community_${timestamp()}`);
const output = path.resolve(
  repoRoot,
  process.env.COMMUNITY_USERS_OUTPUT || `scripts/k6-reports/${prefix}-users.json`
);
const jwtSecret = process.env.JWT_SECRET || "dGhlLXF1aWNrLWJyb3duLWZveC1qdW1wcy1vdmVyLXRoZS1sYXp5LWRvZy1hbmQtc29tZS1vdGhlci1zZWNyZXQtc3R1ZmY=";
const jwtTtlSeconds = positiveInt(process.env.COMMUNITY_JWT_TTL_SECONDS, 24 * 60 * 60);

const connection = await mysql.createConnection({
  host: process.env.MYSQL_HOST || "127.0.0.1",
  port: positiveInt(process.env.MYSQL_PORT, 13306),
  user: process.env.MYSQL_USERNAME || "root",
  password: process.env.MYSQL_PASSWORD || process.env.MYSQL_ROOT_PASSWORD || "test1234",
  database: process.env.MYSQL_DATABASE || "coin_zzickmock",
  timezone: "Z",
});

try {
  const users = [];
  for (let index = 1; index <= count; index += 1) {
    const padded = String(index).padStart(5, "0");
    const account = `${prefix}_${padded}`.slice(0, 64);
    const nickname = `${prefix}_${padded}`.slice(0, 100);
    const email = `${account}@k6.local`;
    const memberName = `K6 Community ${padded}`;
    const phoneNumber = `010-${String(8000 + (index % 1000)).padStart(4, "0")}-${String(index % 10000).padStart(4, "0")}`;

    await connection.execute(
      `INSERT INTO member_credentials
        (account, password_hash, member_name, member_email, phone_number, invest_score, role, nickname, withdrawn_at)
       VALUES (?, NULL, ?, ?, ?, 0, 'USER', ?, NULL)
       ON DUPLICATE KEY UPDATE
         id = LAST_INSERT_ID(id),
         member_name = VALUES(member_name),
         member_email = VALUES(member_email),
         phone_number = VALUES(phone_number),
         role = 'USER',
         nickname = VALUES(nickname),
         withdrawn_at = NULL`,
      [account, memberName, email, phoneNumber, nickname]
    );

    const [idRows] = await connection.execute("SELECT LAST_INSERT_ID() AS id");
    const memberId = Number(idRows[0].id);
    if (!Number.isFinite(memberId) || memberId <= 0) {
      throw new Error(`Failed to resolve member id for ${account}`);
    }

    users.push({
      account,
      accessToken: issueAccessToken({
        memberId,
        account,
        nickname,
        email,
        role: "USER",
      }),
    });
  }

  mkdirSync(path.dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify({ precreatedUsers: users }, null, 2)}\n`);
  console.log(JSON.stringify({
    prefix,
    count: users.length,
    output: path.relative(repoRoot, output),
  }, null, 2));
} finally {
  await connection.end();
}

function issueAccessToken({ memberId, account, nickname, email, role }) {
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: String(memberId),
    iat: now,
    exp: now + jwtTtlSeconds,
    tokenType: "ACCESS",
    memberId,
    nickname,
    email,
    role,
    account,
  };
  const header = {
    alg: hmacJwtAlgorithm(jwtSecret),
    typ: "JWT",
  };
  const encodedHeader = base64Url(JSON.stringify(header));
  const encodedPayload = base64Url(JSON.stringify(payload));
  const signature = createHmac(hmacNodeAlgorithm(header.alg), jwtSecret)
    .update(`${encodedHeader}.${encodedPayload}`)
    .digest("base64url");
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

function hmacJwtAlgorithm(secret) {
  const length = Buffer.byteLength(secret, "utf8");
  if (length >= 64) {
    return "HS512";
  }
  if (length >= 48) {
    return "HS384";
  }
  if (length >= 32) {
    return "HS256";
  }
  throw new Error("JWT_SECRET must be at least 32 bytes for HMAC signing.");
}

function hmacNodeAlgorithm(alg) {
  if (alg === "HS512") {
    return "sha512";
  }
  if (alg === "HS384") {
    return "sha384";
  }
  return "sha256";
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function positiveInt(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function sanitizePrefix(value) {
  return String(value).replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 48);
}

function timestamp() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, "0");
  return [
    now.getUTCFullYear(),
    pad(now.getUTCMonth() + 1),
    pad(now.getUTCDate()),
    pad(now.getUTCHours()),
    pad(now.getUTCMinutes()),
    pad(now.getUTCSeconds()),
  ].join("");
}
