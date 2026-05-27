import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { supportedSymbolsFromEnv } from "./api-contract.js";

const BASE_URL = (__ENV.BASE_URL || "http://host.docker.internal:18080").replace(/\/$/, "");
const SYMBOLS = supportedSymbolsFromEnv();
const RUNS = positiveIntEnv("RUNS", 5);
const THINK_TIME_MS = nonNegativeIntEnv("THINK_TIME_MS", 250);
const REPORT_HTML = __ENV.REPORT_HTML || "/scripts/k6-candle-latency-report.html";

// 프론트엔드 FuturesPriceChart의 interval 설정을 그대로 옮긴 값이다.
// 브라우저가 실제로 보내는 limit와 맞춰야 프론트 화면에서 체감한 응답 지연을 서버 API 단독 측정에서도 비교할 수 있다.
const INTERVALS = [
  { value: "1m", limit: 180, family: "minute", previousFinding: "BTC/ETH 초기 로드와 ETH 1m 전환에서 느림" },
  { value: "3m", limit: 180, family: "minute", previousFinding: "이전 측정에서는 상대적으로 빠른 대조군" },
  { value: "5m", limit: 180, family: "minute", previousFinding: "이전 측정에서는 상대적으로 빠른 대조군" },
  { value: "15m", limit: 180, family: "minute", previousFinding: "이전 측정에서는 상대적으로 빠른 대조군" },
  { value: "1h", limit: 168, family: "hour", previousFinding: "직접 저장된 completed 1h 조회 대조군" },
  { value: "4h", limit: 180, family: "hour-derived", previousFinding: "1h completed row에서 파생되는 시간봉" },
  { value: "12h", limit: 180, family: "hour-derived", previousFinding: "1h completed row에서 파생되는 시간봉" },
  { value: "1D", limit: 180, family: "calendar", previousFinding: "일 경계 rollup 비용 확인" },
  { value: "1W", limit: 104, family: "calendar", previousFinding: "이전 측정에서 BTC/ETH 모두 느렸던 주봉" },
  { value: "1M", limit: 60, family: "calendar", previousFinding: "이전 측정에서 BTC/ETH 모두 느렸던 월봉" },
];

// k6 옵션은 의도적으로 단일 VU, 단일 iteration이다.
// 실제 반복은 default 함수 안에서 순차 loop로 수행하므로 부하 테스트가 아니라 "동일한 사용자 한 명이 화면을 차례로 눌렀을 때"의 서버 응답 시간 측정이 된다.
export const options = {
  scenarios: {
    candle_latency_serial: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      maxDuration: "45m",
    },
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)", "count"],
};

const scenarioDefinitions = buildScenarioDefinitions();
const scenarioMetrics = {};
const scenarioErrorCounters = {};

for (const scenario of scenarioDefinitions) {
  scenarioMetrics[scenario.id] = new Trend(`candle_${scenario.id}_duration_ms`, true);
  scenarioErrorCounters[scenario.id] = new Counter(`candle_${scenario.id}_errors`);
}

export default function () {
  for (let run = 1; run <= RUNS; run += 1) {
    for (const scenario of scenarioDefinitions) {
      executeScenario(scenario, run);
      sleep(THINK_TIME_MS / 1000);
    }
  }
}

function buildScenarioDefinitions() {
  const scenarios = [];

  for (const symbol of SYMBOLS) {
    // 시나리오 1: 심볼 상세 화면 최초 진입.
    // 프론트 기본 interval은 1m이고, 차트 컴포넌트가 첫 history query를 보내는 흐름을 그대로 재현한다.
    scenarios.push({
      id: safeId(`${symbol}_initial_1m`),
      group: "초기 로드",
      name: `${symbol} 상세 첫 차트 로드`,
      symbol,
      interval: "1m",
      limit: intervalConfig("1m").limit,
      mode: "single",
      note: "브라우저 진입 직후 /candles?interval=1m 요청",
    });

    for (const interval of INTERVALS) {
      // 시나리오 2: 사용자가 interval 버튼을 누르는 흐름.
      // 10개 interval을 모두 측정해서 분봉 직접/분봉 파생/시간봉 직접/시간봉 파생/달력봉 파생 비용을 한 표에서 비교한다.
      scenarios.push({
        id: safeId(`${symbol}_switch_${interval.value}`),
        group: "Interval 전환",
        name: `${symbol} ${interval.value} 버튼 전환`,
        symbol,
        interval: interval.value,
        limit: interval.limit,
        mode: "single",
        note: interval.previousFinding,
      });
    }

    for (const interval of INTERVALS) {
      // 시나리오 3: 차트를 왼쪽으로 움직여 더 오래된 캔들을 가져오는 흐름.
      // 프론트 getNextPageParam과 동일하게 첫 페이지의 가장 오래된 openTime을 before cursor로 사용한다.
      // 첫 페이지 요청은 cursor를 얻기 위한 준비 요청이고, 측정 값은 실제 사용자가 pan/scroll 후 받는 before 요청에 기록한다.
      scenarios.push({
        id: safeId(`${symbol}_scroll_${interval.value}`),
        group: "차트 과거 이동",
        name: `${symbol} ${interval.value} 과거 캔들 추가 로드`,
        symbol,
        interval: interval.value,
        limit: interval.limit,
        mode: "scroll",
        note: "차트 왼쪽 이동 시 before cursor 기반 다음 페이지 요청",
      });
    }
  }

  return scenarios;
}

function executeScenario(scenario, run) {
  if (scenario.mode === "scroll") {
    executeScrollScenario(scenario, run);
    return;
  }

  const response = requestCandles(scenario, run, "measured");
  recordScenarioResult(scenario, response);
}

function executeScrollScenario(scenario, run) {
  const seedResponse = requestCandles(scenario, run, "cursor_seed");

  if (!isSuccessfulCandleResponse(seedResponse)) {
    scenarioErrorCounters[scenario.id].add(1);
    return;
  }

  const candles = seedResponse.json("data") || [];
  const before = candles.length > 0 ? candles[0].openTime : null;

  if (!before) {
    scenarioErrorCounters[scenario.id].add(1);
    return;
  }

  const response = requestCandles(scenario, run, "measured_scroll", before);
  recordScenarioResult(scenario, response);
}

function requestCandles(scenario, run, phase, before) {
  const url = candleUrl(scenario.symbol, scenario.interval, scenario.limit, before);
  const tags = {
    scenario: scenario.id,
    scenario_group: scenario.group,
    symbol: scenario.symbol,
    interval: scenario.interval,
    phase,
    run: String(run),
  };

  return http.get(url, {
    tags,
    timeout: "60s",
    headers: {
      Accept: "application/json",
    },
  });
}

function recordScenarioResult(scenario, response) {
  const ok = check(response, {
    "HTTP 200": (res) => res.status === 200,
    "API success true": (res) => res.json("success") === true,
    "캔들 배열 응답": (res) => Array.isArray(res.json("data")),
  });

  scenarioMetrics[scenario.id].add(response.timings.duration, {
    symbol: scenario.symbol,
    interval: scenario.interval,
    group: scenario.group,
  });

  if (!ok) {
    scenarioErrorCounters[scenario.id].add(1);
  }
}

function isSuccessfulCandleResponse(response) {
  return (
    response.status === 200 &&
    response.json("success") === true &&
    Array.isArray(response.json("data"))
  );
}

function candleUrl(symbol, interval, limit, before) {
  let url =
    `${BASE_URL}/api/futures/markets/${encodeURIComponent(symbol)}/candles` +
    `?interval=${encodeURIComponent(interval)}` +
    `&limit=${encodeURIComponent(String(limit))}`;

  if (before) {
    url += `&before=${encodeURIComponent(before)}`;
  }

  return url;
}

export function handleSummary(data) {
  const rows = scenarioDefinitions.map((scenario) => {
    const metric = data.metrics[`candle_${scenario.id}_duration_ms`];
    const errors = data.metrics[`candle_${scenario.id}_errors`];
    const values = metric && metric.values ? metric.values : {};
    const errorValues = errors && errors.values ? errors.values : {};

    return Object.assign({}, scenario, {
      avg: values.avg,
      min: values.min,
      med: values.med,
      max: values.max,
      p90: values["p(90)"],
      p95: values["p(95)"],
      p99: values["p(99)"],
      count: values.count || 0,
      errors: errorValues.count || 0,
    });
  });

  const stdout = renderTextSummary(rows);

  return {
    stdout,
    [REPORT_HTML]: renderHtmlReport(rows, data),
  };
}

function renderTextSummary(rows) {
  const slowest = rows
    .slice()
    .sort((a, b) => numeric(b.p95) - numeric(a.p95))
    .slice(0, 12);

  const lines = [
    "",
    "Candle latency summary",
    `baseUrl=${BASE_URL} runs=${RUNS} symbols=${SYMBOLS.join(",")} thinkTimeMs=${THINK_TIME_MS}`,
    "slowest by p95:",
    "group | scenario | avg | p90 | p95 | p99 | max | count | errors",
  ];

  for (const row of slowest) {
    lines.push(
      [
        row.group,
        row.name,
        formatMs(row.avg),
        formatMs(row.p90),
        formatMs(row.p95),
        formatMs(row.p99),
        formatMs(row.max),
        String(row.count),
        String(row.errors),
      ].join(" | ")
    );
  }

  lines.push("");
  lines.push(`HTML report: ${REPORT_HTML}`);
  lines.push("");
  return lines.join("\n");
}

function renderHtmlReport(rows, data) {
  const generatedAt = new Date().toISOString();
  const slowestRows = rows.slice().sort((a, b) => numeric(b.p95) - numeric(a.p95)).slice(0, 15);
  const groupedRows = rows.slice().sort((a, b) => {
    const groupCompare = a.group.localeCompare(b.group);
    if (groupCompare !== 0) {
      return groupCompare;
    }
    const symbolCompare = a.symbol.localeCompare(b.symbol);
    if (symbolCompare !== 0) {
      return symbolCompare;
    }
    return INTERVALS.findIndex((interval) => interval.value === a.interval) -
      INTERVALS.findIndex((interval) => interval.value === b.interval);
  });

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Coin Zzickmock Candle k6 Latency Report</title>
  <style>
    body { margin: 0; background: #f7f8fa; color: #171923; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    main { max-width: 1280px; margin: 0 auto; padding: 32px 24px 56px; }
    h1 { margin: 0 0 8px; font-size: 28px; line-height: 1.25; }
    h2 { margin: 36px 0 12px; font-size: 20px; }
    p { margin: 6px 0; color: #4a5568; }
    .meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; margin: 22px 0; }
    .meta div { border: 1px solid #e2e8f0; background: #fff; border-radius: 8px; padding: 12px 14px; }
    .meta strong { display: block; color: #2d3748; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
    table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
    th, td { padding: 9px 10px; border-bottom: 1px solid #edf2f7; text-align: right; font-size: 13px; white-space: nowrap; }
    th { background: #edf2f7; color: #2d3748; font-weight: 700; }
    tr:last-child td { border-bottom: 0; }
    td:first-child, th:first-child, td:nth-child(2), th:nth-child(2), td:nth-child(3), th:nth-child(3), td:nth-child(4), th:nth-child(4) { text-align: left; }
    .warn { color: #b7791f; font-weight: 700; }
    .bad { color: #c53030; font-weight: 700; }
    .good { color: #2f855a; font-weight: 700; }
    .note { max-width: 360px; white-space: normal; color: #4a5568; }
    code { background: #edf2f7; border-radius: 5px; padding: 2px 5px; }
  </style>
</head>
<body>
<main>
  <h1>캔들 API k6 응답 시간 리포트</h1>
  <p>동시 트래픽을 넣지 않고 단일 VU가 프론트 차트 동작을 순차적으로 재현한 측정입니다.</p>
  <div class="meta">
    <div><strong>생성 시각</strong>${escapeHtml(generatedAt)}</div>
    <div><strong>대상 URL</strong><code>${escapeHtml(BASE_URL)}</code></div>
    <div><strong>반복 횟수</strong>${RUNS}회</div>
    <div><strong>심볼</strong>${escapeHtml(SYMBOLS.join(", "))}</div>
    <div><strong>시나리오 수</strong>${rows.length}개</div>
    <div><strong>요청 간 대기</strong>${THINK_TIME_MS}ms</div>
  </div>

  <h2>p95 기준 느린 상위 시나리오</h2>
  ${renderRowsTable(slowestRows)}

  <h2>전체 시나리오 결과</h2>
  ${renderRowsTable(groupedRows)}

  <h2>측정 방식</h2>
  <p><strong>초기 로드</strong>: 심볼 상세 페이지 진입 시 기본 <code>1m</code> 차트 요청을 측정했습니다.</p>
  <p><strong>Interval 전환</strong>: 프론트의 10개 interval 버튼이 보내는 limit 값으로 각각 측정했습니다.</p>
  <p><strong>차트 과거 이동</strong>: 첫 페이지의 가장 오래된 <code>openTime</code>을 <code>before</code> cursor로 넣어 다음 페이지 응답 시간을 측정했습니다.</p>
  <p>k6 전체 HTTP 지표의 <code>http_req_duration</code>에는 scroll cursor 생성을 위한 준비 요청도 포함되지만, 표의 시나리오별 값은 실제 측정 대상 요청만 기록합니다.</p>

  <h2>k6 전체 HTTP 요약</h2>
  <p>http_req_duration avg=${formatMs(httpReqValues(data).avg)}, p90=${formatMs(httpReqValues(data)["p(90)"])}, p95=${formatMs(httpReqValues(data)["p(95)"])}, p99=${formatMs(httpReqValues(data)["p(99)"])}, max=${formatMs(httpReqValues(data).max)}</p>
</main>
</body>
</html>`;
}

function renderRowsTable(rows) {
  return `<table>
    <thead>
      <tr>
        <th>그룹</th>
        <th>시나리오</th>
        <th>심볼</th>
        <th>Interval</th>
        <th>평균</th>
        <th>중앙값</th>
        <th>p90</th>
        <th>p95</th>
        <th>p99</th>
        <th>최대</th>
        <th>샘플</th>
        <th>오류</th>
        <th>메모</th>
      </tr>
    </thead>
    <tbody>
      ${rows.map(renderRow).join("\n")}
    </tbody>
  </table>`;
}

function httpReqValues(data) {
  const metric = data.metrics.http_req_duration;
  return metric && metric.values ? metric.values : {};
}

function renderRow(row) {
  const p95Class = numeric(row.p95) >= 1000 ? "bad" : numeric(row.p95) >= 500 ? "warn" : "good";
  const errorClass = row.errors > 0 ? "bad" : "good";

  return `<tr>
    <td>${escapeHtml(row.group)}</td>
    <td>${escapeHtml(row.name)}</td>
    <td>${escapeHtml(row.symbol)}</td>
    <td>${escapeHtml(row.interval)}</td>
    <td>${formatMs(row.avg)}</td>
    <td>${formatMs(row.med)}</td>
    <td>${formatMs(row.p90)}</td>
    <td class="${p95Class}">${formatMs(row.p95)}</td>
    <td>${formatMs(row.p99)}</td>
    <td>${formatMs(row.max)}</td>
    <td>${row.count}</td>
    <td class="${errorClass}">${row.errors}</td>
    <td class="note">${escapeHtml(row.note)}</td>
  </tr>`;
}

function intervalConfig(value) {
  return INTERVALS.find((interval) => interval.value === value);
}

function positiveIntEnv(name, fallback) {
  const value = Number.parseInt(__ENV[name] || String(fallback), 10);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

function nonNegativeIntEnv(name, fallback) {
  const value = Number.parseInt(__ENV[name] || String(fallback), 10);
  return Number.isFinite(value) && value >= 0 ? value : fallback;
}

function safeId(value) {
  return value.replace(/[^A-Za-z0-9_]/g, "_");
}

function numeric(value) {
  return Number.isFinite(value) ? value : 0;
}

function formatMs(value) {
  if (!Number.isFinite(value)) {
    return "-";
  }
  return `${value.toFixed(1)} ms`;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
