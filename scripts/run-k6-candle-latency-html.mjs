#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "..");
const k6ScriptPath = path.join(repoRoot, "scripts", "k6-candle-latency.js");

const runner = (process.env.K6_RUNNER || "auto").toLowerCase();
const dockerImage = process.env.K6_DOCKER_IMAGE || "grafana/k6:0.49.0";
const reportDir = path.resolve(repoRoot, process.env.K6_REPORT_DIR || "scripts/k6-reports");
const reportPath = nextReportPath(reportDir);
const reportPathInContainer = toContainerPath(reportPath);
const k6ScriptPathInContainer = toContainerPath(k6ScriptPath);
const passthroughEnvNames = ["BASE_URL", "SYMBOLS", "RUNS", "THINK_TIME_MS"];
const isDryRun = process.argv.includes("--dry-run");

mkdirSync(reportDir, { recursive: true });

const command = buildCommand();

console.log(`HTML report: ${reportPath}`);
console.log(`Runner: ${command.label}`);

if (isDryRun) {
  console.log(command.commandLine);
  process.exit(0);
}

const result = spawnSync(command.bin, command.args, {
  cwd: repoRoot,
  env: command.env,
  stdio: "inherit",
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

if (result.signal) {
  console.error(`k6 run stopped by signal: ${result.signal}`);
  process.exit(1);
}

process.exit(result.status ?? 1);

function buildCommand() {
  if (runner === "docker") {
    return dockerCommand();
  }

  if (runner === "local") {
    return localCommand();
  }

  if (commandExists("docker")) {
    return dockerCommand();
  }

  if (commandExists("k6")) {
    return localCommand();
  }

  console.error("Docker or local k6 is required. Install Docker/k6 or set K6_RUNNER=docker|local.");
  process.exit(1);
}

function dockerCommand() {
  const args = [
    "run",
    "--rm",
    "-i",
    "-v",
    `${repoRoot}:/workspace`,
    "-w",
    "/workspace",
    "-e",
    `REPORT_HTML=${reportPathInContainer}`,
  ];

  for (const name of passthroughEnvNames) {
    if (process.env[name]) {
      args.push("-e", `${name}=${process.env[name]}`);
    }
  }

  args.push(dockerImage, "run", k6ScriptPathInContainer);

  return {
    label: `docker (${dockerImage})`,
    bin: "docker",
    args,
    env: process.env,
    commandLine: shellJoin(["docker", ...args]),
  };
}

function localCommand() {
  const env = { ...process.env, REPORT_HTML: reportPath };
  const args = ["run", k6ScriptPath];

  return {
    label: "local k6",
    bin: "k6",
    args,
    env,
    commandLine: shellJoin([`REPORT_HTML=${reportPath}`, "k6", ...args]),
  };
}

function nextReportPath(dir) {
  const baseName = `k6-candle-latency-${timestamp(new Date())}`;
  let candidate = path.join(dir, `${baseName}.html`);
  let suffix = 2;

  while (existsSync(candidate)) {
    candidate = path.join(dir, `${baseName}-${suffix}.html`);
    suffix += 1;
  }

  return candidate;
}

function timestamp(date) {
  const pad = (value, length = 2) => String(value).padStart(length, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    "-",
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
    "-",
    pad(date.getMilliseconds(), 3),
  ].join("");
}

function toContainerPath(hostPath) {
  const relative = path.relative(repoRoot, hostPath);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    console.error(`Path must be inside repository for Docker runner: ${hostPath}`);
    process.exit(1);
  }
  return `/workspace/${relative.split(path.sep).join("/")}`;
}

function commandExists(command) {
  const result = spawnSync(command, ["--version"], { stdio: "ignore" });
  return result.status === 0;
}

function shellJoin(parts) {
  return parts.map(shellQuote).join(" ");
}

function shellQuote(value) {
  const text = String(value);
  if (/^[A-Za-z0-9_@%+=:,./-]+$/.test(text)) {
    return text;
  }
  return `'${text.replace(/'/g, "'\\''")}'`;
}
