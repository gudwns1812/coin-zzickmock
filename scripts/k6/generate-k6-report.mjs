#!/usr/bin/env node
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { renderHtmlReport, renderMarkdownReport } from "./k6-report.mjs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "../..");

const targetArg = process.argv[2];
if (!targetArg) {
  console.error("Usage: node scripts/k6/generate-k6-report.mjs <summary-json-or-report-dir> [output-dir]");
  process.exit(1);
}

const targetPath = path.resolve(repoRoot, targetArg);
const outputDir = path.resolve(repoRoot, process.argv[3] || targetArg);

if (!existsSync(targetPath)) {
  console.error(`Not found: ${targetPath}`);
  process.exit(1);
}

mkdirSync(outputDir, { recursive: true });

const summaries = loadSummaries(targetPath);
if (summaries.length === 0) {
  console.error(`No *-summary.json files found in ${targetPath}`);
  process.exit(1);
}

const generated = [];
for (const summary of summaries) {
  const data = JSON.parse(readFileSync(summary.path, "utf8"));
  const title = `k6 ${summary.name}`;
  const baseName = summary.name.replace(/-summary$/, "");
  const mdPath = path.join(outputDir, `${baseName}-report.md`);
  const htmlPath = path.join(outputDir, `${baseName}-report.html`);

  writeFileSync(mdPath, renderMarkdownReport(data, title));
  writeFileSync(htmlPath, renderHtmlReport(data, title));
  generated.push({ name: summary.name, mdPath, htmlPath });
}

if (generated.length > 1) {
  const indexMdPath = path.join(outputDir, "index.md");
  const indexHtmlPath = path.join(outputDir, "index.html");
  writeFileSync(indexMdPath, renderIndexMarkdown(generated, outputDir));
  writeFileSync(indexHtmlPath, renderIndexHtml(generated, outputDir));
  generated.push({ name: "index", mdPath: indexMdPath, htmlPath: indexHtmlPath });
}

for (const item of generated) {
  console.log(`${item.name}:`);
  console.log(`  ${path.relative(repoRoot, item.mdPath)}`);
  console.log(`  ${path.relative(repoRoot, item.htmlPath)}`);
}

function loadSummaries(fileOrDir) {
  const statTarget = readFileSyncOrNull(fileOrDir);
  if (statTarget && fileOrDir.endsWith(".json")) {
    return [{ name: path.basename(fileOrDir, ".json"), path: fileOrDir }];
  }

  return readdirSync(fileOrDir)
    .filter((fileName) => fileName.endsWith("-summary.json"))
    .sort()
    .map((fileName) => ({
      name: path.basename(fileName, ".json"),
      path: path.join(fileOrDir, fileName),
    }));
}

function readFileSyncOrNull(filePath) {
  try {
    return readFileSync(filePath);
  } catch {
    return null;
  }
}

function renderIndexMarkdown(items, dir) {
  const lines = ["# k6 Report Index", ""];
  for (const item of items) {
    lines.push(`- [${item.name}](${path.relative(dir, item.htmlPath)}) / [md](${path.relative(dir, item.mdPath)})`);
  }
  lines.push("");
  return lines.join("\n");
}

function renderIndexHtml(items, dir) {
  const rows = items.map((item) => {
    const html = escapeHtml(path.relative(dir, item.htmlPath));
    const md = escapeHtml(path.relative(dir, item.mdPath));
    return `<li><a href="${html}">${escapeHtml(item.name)}</a> <span>/</span> <a href="${md}">md</a></li>`;
  }).join("\n");

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>k6 Report Index</title>
  <style>
    body { margin: 0; background: #f7f8fa; color: #17202a; font: 15px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    main { max-width: 840px; margin: 0 auto; padding: 40px 20px; }
    h1 { margin-top: 0; }
    li { margin: 8px 0; }
    a { color: #1d4ed8; }
    span { color: #6b7280; }
  </style>
</head>
<body><main><h1>k6 Report Index</h1><ul>${rows}</ul></main></body>
</html>
`;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
