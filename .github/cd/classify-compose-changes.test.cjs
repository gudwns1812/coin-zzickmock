const assert = require("node:assert/strict");
const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const script = path.resolve(__dirname, "classify-compose-changes.cjs");

function run(command, args, cwd) {
  return execFileSync(command, args, { cwd, encoding: "utf8" }).trim();
}

function withRepo(initial, update, callback) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "compose-classifier-"));
  try {
    run("git", ["init"], dir);
    run("git", ["config", "user.email", "test@example.com"], dir);
    run("git", ["config", "user.name", "Test"], dir);
    fs.writeFileSync(path.join(dir, "docker-compose.prod.yml"), initial);
    run("git", ["add", "docker-compose.prod.yml"], dir);
    run("git", ["commit", "-m", "initial"], dir);
    const before = run("git", ["rev-parse", "HEAD"], dir);
    fs.writeFileSync(path.join(dir, "docker-compose.prod.yml"), update);
    run("git", ["add", "docker-compose.prod.yml"], dir);
    run("git", ["commit", "-m", "update"], dir);
    const after = run("git", ["rev-parse", "HEAD"], dir);
    callback({ dir, before, after });
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

function classify(repo) {
  return JSON.parse(run("node", [script, "--before", repo.before, "--after", repo.after], repo.dir));
}

function classifyFails(repo) {
  assert.throws(() => run("node", [script, "--before", repo.before, "--after", repo.after], repo.dir), {
    status: 1,
  });
}

const baseCompose = `name: coin-zzickmock

services:
  redis:
    image: redis:7.4-alpine
    command: [ "redis-server" ]

  backend:
    image: \${BACKEND_IMAGE:?Set BACKEND_IMAGE}
    environment:
      LOG_LEVEL_APP: info

  nginx:
    image: nginx:1.27-alpine
    volumes:
      - ./infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro

networks:
  coin:
    driver: bridge
`;

test("classifies backend service changes as backend runtime", () => {
  withRepo(baseCompose, baseCompose.replace("LOG_LEVEL_APP: info", "LOG_LEVEL_APP: debug"), (repo) => {
    assert.deepEqual(classify(repo), {
      backend_runtime: true,
      infra_runtime: false,
      nginx_service_definition: false,
    });
  });
});

test("classifies non-backend service changes as infra runtime", () => {
  withRepo(baseCompose, baseCompose.replace("redis:7.4-alpine", "redis:7.4.1-alpine"), (repo) => {
    assert.deepEqual(classify(repo), {
      backend_runtime: false,
      infra_runtime: true,
      nginx_service_definition: false,
    });
  });
});

test("marks nginx service definition changes", () => {
  withRepo(baseCompose, baseCompose.replace("nginx:1.27-alpine", "nginx:1.27.1-alpine"), (repo) => {
    assert.deepEqual(classify(repo), {
      backend_runtime: false,
      infra_runtime: true,
      nginx_service_definition: true,
    });
  });
});

test("comment-only changes produce no effect", () => {
  withRepo(baseCompose, baseCompose.replace("services:", "services:\n  # no-op comment"), (repo) => {
    assert.deepEqual(classify(repo), {
      backend_runtime: false,
      infra_runtime: false,
      nginx_service_definition: false,
    });
  });
});

test("top-level name changes fail ambiguous", () => {
  withRepo(baseCompose, baseCompose.replace("name: coin-zzickmock", "name: coin-zzickmock-prod"), classifyFails);
});

test("top-level volume changes fail ambiguous", () => {
  const next = `${baseCompose}
volumes:
  redis-data:
`;
  withRepo(baseCompose, next, classifyFails);
});

test("deleted-only service field changes fail ambiguous", () => {
  const next = baseCompose.replace("    command: [ \"redis-server\" ]\n", "");
  withRepo(baseCompose, next, classifyFails);
});

test("service block header additions fail ambiguous", () => {
  const next = baseCompose.replace(
    "\n  backend:",
    "\n  metrics:\n    image: prom/prometheus:v3.0.1\n\n  backend:",
  );
  withRepo(baseCompose, next, classifyFails);
});

test("yaml anchors fail ambiguous", () => {
  const next = baseCompose.replace("image: redis:7.4-alpine", "image: &redis_image redis:7.4-alpine");
  withRepo(baseCompose, next, classifyFails);
});
