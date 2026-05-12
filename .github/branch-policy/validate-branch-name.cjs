#!/usr/bin/env node

const {execSync} = require("node:child_process");

const allowedTypes = [
    "feat",
    "fix",
    "refactor",
    "docs",
    "test",
    "chore",
    "ci",
    "perf",
    "style",
    "build",
    "revert",
    "dev"
];
const protectedBranches = new Set(["main", "master", "develop"]);
const typePattern = allowedTypes.join("|");
const branchPattern = new RegExp(
    `^(${typePattern})/[a-z0-9][a-z0-9._-]*(/[a-z0-9][a-z0-9._-]*)*$`,
);

function currentBranch() {
    try {
        return execSync("git branch --show-current", {encoding: "utf8"}).trim();
    } catch {
        return "";
    }
}

const branchName = (
    process.argv[2] ||
    process.env.BRANCH_NAME ||
    process.env.GITHUB_HEAD_REF ||
    process.env.GITHUB_REF_NAME ||
    currentBranch()
).trim();

function fail(message) {
    console.error(`Branch name policy failed: ${message}`);
    console.error("");
    console.error("Required format: <type>/<kebab-case-summary>");
    console.error(`Allowed types: ${allowedTypes.join(", ")}`);
    console.error("Examples: feat/limit-order-entry, fix/login-token-refresh, refactor/market-cache-boundary");
    console.error("Forbidden: codex/*, codex-*, untyped names, uppercase letters, spaces");
    process.exit(1);
}

if (!branchName) {
    fail("could not determine the branch name.");
}

if (protectedBranches.has(branchName)) {
    process.exit(0);
}

if (/^codex([/-]|$)/.test(branchName) || /\/codex([/-]|$)/.test(branchName) || /^\[codex\]/.test(branchName)) {
    fail(`'${branchName}' uses the forbidden codex automation prefix.`);
}

if (!branchPattern.test(branchName)) {
    fail(`'${branchName}' does not match the required type-prefixed format.`);
}
