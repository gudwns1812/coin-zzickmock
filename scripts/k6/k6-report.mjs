const DEFAULT_REPORT_PREFIX = "k6-summary";

export function handleSummary(data) {
  const prefix = (__ENV.REPORT_PREFIX || DEFAULT_REPORT_PREFIX).replace(/[^A-Za-z0-9._/-]/g, "_");
  const title = __ENV.REPORT_TITLE || "k6 Load Test Summary";
  const markdown = renderMarkdownReport(data, title);
  const html = renderHtmlReport(data, title);

  return {
    stdout: renderConsoleSummary(data, title),
    [`${prefix}.md`]: markdown,
    [`${prefix}.html`]: html,
  };
}

export function renderMarkdownReport(data, title = "k6 Load Test Summary") {
  const summary = buildSummary(data);
  const lines = [];

  lines.push(`# ${title}`);
  lines.push("");
  lines.push(`- Result: **${summary.thresholdsFailed > 0 ? "FAIL" : "PASS"}**`);
  lines.push(`- Checks: **${formatPercent(summary.checkRate)}** (${summary.checkPasses}/${summary.checkTotal})`);
  lines.push(`- HTTP failure rate: **${formatPercent(summary.httpFailRate)}**`);
  lines.push(`- HTTP p95: **${formatMs(summary.httpDurationP95)}**`);
  lines.push(`- Requests: **${formatNumber(summary.httpReqs)}**`);
  lines.push(`- Iterations: **${formatNumber(summary.iterations)}**`);
  lines.push("");

  lines.push("## Key Metrics");
  lines.push("");
  lines.push("| Metric | Count/Rate | Avg | P90 | P95 | P99 | Max | Threshold |");
  lines.push("| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |");
  for (const row of summary.metricRows) {
    lines.push(
      `| ${escapeMarkdown(row.name)} | ${row.countOrRate} | ${row.avg} | ${row.p90} | ${row.p95} | ${row.p99} | ${row.max} | ${row.threshold} |`
    );
  }
  lines.push("");

  if (summary.failedThresholdRows.length > 0) {
    lines.push("## Failed Thresholds");
    lines.push("");
    lines.push("| Metric | Thresholds |");
    lines.push("| --- | --- |");
    for (const row of summary.failedThresholdRows) {
      lines.push(`| ${escapeMarkdown(row.name)} | ${escapeMarkdown(row.thresholds.join(", "))} |`);
    }
    lines.push("");
  }

  const checkRows = collectChecks(data.root_group);
  if (checkRows.length > 0) {
    lines.push("## Checks");
    lines.push("");
    lines.push("| Check | Pass | Fail | Success |");
    lines.push("| --- | ---: | ---: | ---: |");
    for (const row of checkRows) {
      lines.push(`| ${escapeMarkdown(row.path)} | ${row.passes} | ${row.fails} | ${formatPercent(row.rate)} |`);
    }
    lines.push("");
  }

  return `${lines.join("\n")}\n`;
}

export function renderHtmlReport(data, title = "k6 Load Test Summary") {
  const summary = buildSummary(data);
  const checkRows = collectChecks(data.root_group);
  const statusClass = summary.thresholdsFailed > 0 ? "fail" : "pass";
  const statusLabel = summary.thresholdsFailed > 0 ? "FAIL" : "PASS";

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)}</title>
  <style>
    :root { color-scheme: light; --bg: #f7f8fa; --panel: #fff; --text: #17202a; --muted: #6b7280; --line: #d9dee7; --good: #0f8a4b; --bad: #c0392b; --warn: #a16207; }
    body { margin: 0; background: var(--bg); color: var(--text); font: 14px/1.55 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    main { max-width: 1180px; margin: 0 auto; padding: 32px 20px 48px; }
    h1 { margin: 0 0 18px; font-size: 28px; letter-spacing: 0; }
    h2 { margin: 28px 0 12px; font-size: 18px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
    .card { background: var(--panel); border: 1px solid var(--line); border-radius: 8px; padding: 14px 16px; }
    .label { color: var(--muted); font-size: 12px; text-transform: uppercase; }
    .value { margin-top: 6px; font-size: 22px; font-weight: 700; }
    .pass { color: var(--good); }
    .fail { color: var(--bad); }
    table { width: 100%; border-collapse: collapse; background: var(--panel); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
    th, td { padding: 9px 10px; border-bottom: 1px solid var(--line); text-align: right; vertical-align: top; }
    th:first-child, td:first-child { text-align: left; }
    tr:last-child td { border-bottom: 0; }
    th { background: #eef1f6; color: #374151; font-size: 12px; }
    code { background: #eef1f6; border-radius: 4px; padding: 1px 4px; }
    .muted { color: var(--muted); }
  </style>
</head>
<body>
<main>
  <h1>${escapeHtml(title)}</h1>
  <section class="grid">
    ${statCard("Result", statusLabel, statusClass)}
    ${statCard("Checks", `${formatPercent(summary.checkRate)} (${summary.checkPasses}/${summary.checkTotal})`)}
    ${statCard("HTTP Failures", formatPercent(summary.httpFailRate), summary.httpFailRate > 0 ? "fail" : "pass")}
    ${statCard("HTTP p95", formatMs(summary.httpDurationP95))}
    ${statCard("Requests", formatNumber(summary.httpReqs))}
    ${statCard("Iterations", formatNumber(summary.iterations))}
  </section>

  <h2>Key Metrics</h2>
  ${metricTable(summary.metricRows)}

  ${summary.failedThresholdRows.length > 0 ? `<h2>Failed Thresholds</h2>${thresholdTable(summary.failedThresholdRows)}` : ""}

  ${checkRows.length > 0 ? `<h2>Checks</h2>${checksTable(checkRows)}` : ""}
</main>
</body>
</html>
`;
}

export function renderConsoleSummary(data, title = "k6 Load Test Summary") {
  const summary = buildSummary(data);
  return [
    "",
    `${title}`,
    `Result: ${summary.thresholdsFailed > 0 ? "FAIL" : "PASS"}`,
    `Checks: ${formatPercent(summary.checkRate)} (${summary.checkPasses}/${summary.checkTotal})`,
    `HTTP failure rate: ${formatPercent(summary.httpFailRate)}`,
    `HTTP p95: ${formatMs(summary.httpDurationP95)}`,
    `Requests: ${formatNumber(summary.httpReqs)}`,
    `Iterations: ${formatNumber(summary.iterations)}`,
    "",
  ].join("\n");
}

function buildSummary(data) {
  const metrics = data.metrics || {};
  const checks = metricValues(metrics.checks);
  const httpReqFailed = metricValues(metrics.http_req_failed);
  const httpReqDuration = metricValues(metrics.http_req_duration);
  const httpReqs = metricValues(metrics.http_reqs).count;
  const iterations = metricValues(metrics.iterations).count;
  const checkPasses = numberOrZero(checks.passes);
  const checkFails = numberOrZero(checks.fails);
  const checkTotal = checkPasses + checkFails;
  const metricRows = collectMetricRows(metrics);
  const failedThresholdRows = collectFailedThresholdRows(metrics);

  return {
    checkPasses,
    checkTotal,
    checkRate: checkTotal > 0 ? checkPasses / checkTotal : null,
    httpFailRate: typeof httpReqFailed.value === "number" ? httpReqFailed.value : null,
    httpDurationP95: httpReqDuration["p(95)"],
    httpReqs,
    iterations,
    metricRows,
    failedThresholdRows,
    thresholdsFailed: failedThresholdRows.length,
  };
}

function collectMetricRows(metrics) {
  const preferred = [
    "http_req_duration",
    "http_req_failed",
    "http_reqs",
    "checks",
    "iterations",
  ];
  const custom = Object.keys(metrics)
    .filter((name) => !preferred.includes(name))
    .filter((name) => {
      const values = metricValues(metrics[name]);
      return values && (name.includes("_ms") || name.includes("fail") || name.includes("duration"));
    })
    .sort();

  return [...preferred, ...custom]
    .filter((name) => Object.keys(metricValues(metrics[name])).length > 0)
    .map((name) => metricRow(name, metrics[name]));
}

function metricRow(name, metric) {
  const values = metricValues(metric);
  const thresholds = metric.thresholds || values.thresholds || {};
  return {
    name,
    countOrRate: formatCountOrRate(values),
    avg: formatMs(values.avg),
    p90: formatMs(values["p(90)"]),
    p95: formatMs(values["p(95)"]),
    p99: formatMs(values["p(99)"]),
    max: formatMs(values.max),
    threshold: formatThresholds(thresholds, values),
  };
}

function collectFailedThresholdRows(metrics) {
  return Object.keys(metrics)
    .map((name) => {
      const metric = metrics[name] || {};
      const values = metricValues(metric);
      const thresholds = metric.thresholds || values.thresholds || {};
      return {
        name,
        thresholds: Object.keys(thresholds).filter((key) => thresholdFailed(key, thresholds[key], values)),
      };
    })
    .filter((row) => row.thresholds.length > 0)
    .sort((a, b) => a.name.localeCompare(b.name));
}

function metricValues(metric) {
  if (!metric) {
    return {};
  }
  return metric.values || metric;
}

function collectChecks(group, prefix = "") {
  if (!group) {
    return [];
  }
  const rows = [];
  const groupName = group.name ? `${prefix}${group.name}` : prefix;
  const checks = group.checks || {};
  for (const name of Object.keys(checks).sort()) {
    const check = checks[name];
    const total = numberOrZero(check.passes) + numberOrZero(check.fails);
    rows.push({
      path: check.path || `${groupName}::${name}`,
      passes: numberOrZero(check.passes),
      fails: numberOrZero(check.fails),
      rate: total > 0 ? numberOrZero(check.passes) / total : null,
    });
  }
  const groups = group.groups || {};
  for (const name of Object.keys(groups).sort()) {
    rows.push(...collectChecks(groups[name], groupName ? `${groupName}::` : ""));
  }
  return rows;
}

function statCard(label, value, className = "") {
  return `<div class="card"><div class="label">${escapeHtml(label)}</div><div class="value ${className}">${escapeHtml(value)}</div></div>`;
}

function metricTable(rows) {
  return `<table><thead><tr><th>Metric</th><th>Count/Rate</th><th>Avg</th><th>P90</th><th>P95</th><th>P99</th><th>Max</th><th>Threshold</th></tr></thead><tbody>${rows.map((row) => `<tr><td><code>${escapeHtml(row.name)}</code></td><td>${row.countOrRate}</td><td>${row.avg}</td><td>${row.p90}</td><td>${row.p95}</td><td>${row.p99}</td><td>${row.max}</td><td>${escapeHtml(row.threshold)}</td></tr>`).join("")}</tbody></table>`;
}

function thresholdTable(rows) {
  return `<table><thead><tr><th>Metric</th><th>Thresholds</th></tr></thead><tbody>${rows.map((row) => `<tr><td><code>${escapeHtml(row.name)}</code></td><td>${escapeHtml(row.thresholds.join(", "))}</td></tr>`).join("")}</tbody></table>`;
}

function checksTable(rows) {
  return `<table><thead><tr><th>Check</th><th>Pass</th><th>Fail</th><th>Success</th></tr></thead><tbody>${rows.map((row) => `<tr><td><code>${escapeHtml(row.path)}</code></td><td>${row.passes}</td><td class="${row.fails > 0 ? "fail" : "pass"}">${row.fails}</td><td>${formatPercent(row.rate)}</td></tr>`).join("")}</tbody></table>`;
}

function formatCountOrRate(values) {
  if (typeof values.count === "number") {
    return formatNumber(values.count);
  }
  if (typeof values.rate === "number") {
    return `${formatNumber(values.rate)}/s`;
  }
  if (typeof values.value === "number") {
    return formatPercent(values.value);
  }
  return "-";
}

function formatThresholds(thresholds, values) {
  const keys = Object.keys(thresholds || {});
  if (keys.length === 0) {
    return "-";
  }
  return keys.map((key) => `${thresholdFailed(key, thresholds[key], values) ? "FAIL" : "PASS"} ${key}`).join(", ");
}

function thresholdFailed(expression, legacyValue, values) {
  const evaluated = evaluateThresholdExpression(expression, values || {});
  if (evaluated !== null) {
    return !evaluated;
  }

  // k6 summary-export for tagged thresholds does not always include a values object.
  // In those JSON files, `true` has historically represented a crossed threshold.
  return legacyValue === true;
}

function evaluateThresholdExpression(expression, values) {
  const match = String(expression).match(/^([A-Za-z0-9_()]+)\s*(<=|>=|<|>|==|===)\s*(-?\d+(?:\.\d+)?)$/);
  if (!match) {
    return null;
  }

  const key = match[1];
  const operator = match[2];
  const expected = Number(match[3]);
  const actual = values[key];
  if (typeof actual !== "number" || !Number.isFinite(actual)) {
    return null;
  }

  if (operator === "<") return actual < expected;
  if (operator === "<=") return actual <= expected;
  if (operator === ">") return actual > expected;
  if (operator === ">=") return actual >= expected;
  if (operator === "==" || operator === "===") return actual === expected;
  return null;
}

function formatMs(value) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "-";
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(2)}s`;
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)}ms`;
}

function formatPercent(value) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "-";
  }
  return `${(value * 100).toFixed(2)}%`;
}

function formatNumber(value) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "-";
  }
  const rounded = Math.abs(value % 1) > 0 ? value.toFixed(2).replace(/\.?0+$/, "") : String(value);
  const parts = rounded.split(".");
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return parts.join(".");
}

function numberOrZero(value) {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function escapeMarkdown(value) {
  return String(value).replace(/\|/g, "\\|");
}
