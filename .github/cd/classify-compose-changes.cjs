#!/usr/bin/env node

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

function parseArgs(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith("--")) {
      continue;
    }
    const key = arg.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith("--")) {
      args[key] = "true";
    } else {
      args[key] = value;
      index += 1;
    }
  }
  return args;
}

function runGit(args) {
  return execFileSync("git", args, { encoding: "utf8" });
}

function readRevisionFile(revision, filePath) {
  try {
    return runGit(["show", `${revision}:${filePath}`]).split(/\r?\n/);
  } catch {
    return [];
  }
}

function parseHunks(diff) {
  const hunks = [];
  const hunkPattern = /^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/;
  let current = null;
  let oldLine = 0;
  let newLine = 0;

  for (const line of diff.split(/\r?\n/)) {
    const hunkMatch = line.match(hunkPattern);
    if (hunkMatch) {
      current = {
        oldStart: Number(hunkMatch[1]),
        oldCount: Number(hunkMatch[2] || "1"),
        newStart: Number(hunkMatch[3]),
        newCount: Number(hunkMatch[4] || "1"),
        changes: [],
      };
      hunks.push(current);
      oldLine = current.oldStart;
      newLine = current.newStart;
      continue;
    }

    if (!current || line.length === 0) {
      continue;
    }

    const marker = line[0];
    const text = line.slice(1);
    if (marker === " ") {
      oldLine += 1;
      newLine += 1;
    } else if (marker === "-") {
      current.changes.push({ side: "old", lineNumber: oldLine, text });
      oldLine += 1;
    } else if (marker === "+") {
      current.changes.push({ side: "new", lineNumber: newLine, text });
      newLine += 1;
    }
  }

  return hunks;
}

function buildServiceLineMap(lines) {
  const serviceByLine = new Map();
  let inServices = false;
  let currentService = null;

  for (let index = 0; index < lines.length; index += 1) {
    const lineNumber = index + 1;
    const line = lines[index] || "";

    if (/^\S/.test(line) && !/^services:\s*(?:#.*)?$/.test(line)) {
      inServices = false;
      currentService = null;
    }

    if (/^services:\s*(?:#.*)?$/.test(line)) {
      inServices = true;
      currentService = null;
      continue;
    }

    if (!inServices) {
      continue;
    }

    const serviceMatch = line.match(/^  ([A-Za-z0-9._-]+):\s*(?:#.*)?$/);
    if (serviceMatch) {
      currentService = serviceMatch[1];
      serviceByLine.set(lineNumber, currentService);
      continue;
    }

    if (/^  \S/.test(line)) {
      currentService = null;
      continue;
    }

    if (currentService && /^(    |\s*$)/.test(line)) {
      serviceByLine.set(lineNumber, currentService);
    }
  }

  return serviceByLine;
}

function isIgnorable(text) {
  return text.trim() === "" || text.trim().startsWith("#");
}

function hasAmbiguousYamlFeature(text) {
  const trimmed = text.trim();
  return /(^|\s)<<\s*:/.test(trimmed) || /(^|\s)&[A-Za-z0-9_-]+/.test(trimmed) || /(^|\s)\*[A-Za-z0-9_-]+/.test(trimmed);
}

function topLevelKey(text) {
  const match = text.match(/^([A-Za-z0-9._-]+):(?:\s|$)/);
  return match ? match[1] : null;
}

function serviceHeader(text) {
  const match = text.match(/^  ([A-Za-z0-9._-]+):\s*(?:#.*)?$/);
  return match ? match[1] : null;
}

function classify({ before, after, filePath }) {
  const diff = runGit(["diff", "--unified=0", before, after, "--", filePath]);
  if (!diff.trim()) {
    return emptyResult();
  }

  const oldLines = readRevisionFile(before, filePath);
  const newLines = readRevisionFile(after, filePath);
  const oldServices = buildServiceLineMap(oldLines);
  const newServices = buildServiceLineMap(newLines);
  const result = emptyResult();

  for (const hunk of parseHunks(diff)) {
    const meaningfulChanges = hunk.changes.filter((change) => !isIgnorable(change.text));
    const hasMeaningfulAddition = meaningfulChanges.some((change) => change.side === "new");
    const hasMeaningfulDeletion = meaningfulChanges.some((change) => change.side === "old");

    if (hasMeaningfulDeletion && !hasMeaningfulAddition) {
      fail("Deleted-only compose hunks require manual workflow_dispatch scope.");
    }

    for (const change of hunk.changes) {
      if (isIgnorable(change.text)) {
        continue;
      }

      if (hasAmbiguousYamlFeature(change.text)) {
        fail(`Ambiguous compose change uses YAML anchor/alias/merge syntax at ${change.side}:${change.lineNumber}: ${change.text.trim()}`);
      }

      const key = topLevelKey(change.text);
      if (key) {
        if (["name", "networks", "volumes"].includes(key)) {
          fail(`Ambiguous top-level compose '${key}' change requires manual workflow_dispatch scope.`);
        }
        if (key !== "services") {
          fail(`Ambiguous top-level compose '${key}' change requires manual workflow_dispatch scope.`);
        }
      }

      const header = serviceHeader(change.text);
      if (header) {
        fail(`Ambiguous services.${header} block add/remove/rename requires manual workflow_dispatch scope.`);
      }

      const map = change.side === "new" ? newServices : oldServices;
      const service = map.get(change.lineNumber);
      if (!service) {
        fail(`Could not map compose change at ${change.side}:${change.lineNumber} to exactly one concrete services.<name> block.`);
      }

      if (service === "backend") {
        result.backend_runtime = true;
      } else {
        result.infra_runtime = true;
      }

      if (service === "nginx") {
        result.nginx_service_definition = true;
      }
    }
  }

  return result;
}

function emptyResult() {
  return {
    backend_runtime: false,
    infra_runtime: false,
    nginx_service_definition: false,
  };
}

function fail(message) {
  const error = new Error(message);
  error.classificationError = true;
  throw error;
}

function writeGithubOutput(result) {
  const outputPath = process.env.GITHUB_OUTPUT;
  if (!outputPath) {
    return;
  }

  const lines = [
    `backend_runtime=${result.backend_runtime}`,
    `infra_runtime=${result.infra_runtime}`,
    `nginx_service_definition=${result.nginx_service_definition}`,
  ];
  fs.appendFileSync(outputPath, `${lines.join("\n")}\n`);
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const before = args.before;
  const after = args.after;
  const filePath = args.file || "docker-compose.prod.yml";

  if (!before || !after) {
    console.error("Usage: classify-compose-changes.cjs --before <sha> --after <sha> [--file docker-compose.prod.yml]");
    process.exit(2);
  }

  try {
    const result = classify({ before, after, filePath: path.normalize(filePath) });
    writeGithubOutput(result);
    process.stdout.write(`${JSON.stringify(result)}\n`);
  } catch (error) {
    const message = error && error.message ? error.message : String(error);
    console.error(`::error title=Ambiguous docker-compose.prod.yml change::${message}`);
    process.exit(error.classificationError ? 1 : 2);
  }
}

if (require.main === module) {
  main();
}

module.exports = {
  buildServiceLineMap,
  classify,
  hasAmbiguousYamlFeature,
  parseHunks,
  serviceHeader,
};
