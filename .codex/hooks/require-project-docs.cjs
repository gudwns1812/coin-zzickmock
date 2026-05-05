#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

function readStdin() {
  try {
    return fs.readFileSync(0, "utf8");
  } catch {
    return "";
  }
}

function safeJsonParse(text) {
  if (!text.trim()) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
}

function firstText(...values) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return "";
}

function repoRoot(payload) {
  const cwd = firstText(payload.cwd, process.cwd());
  return path.resolve(cwd);
}

function hasAny(text, patterns) {
  return patterns.some((pattern) => pattern.test(text));
}

function addUnique(target, values) {
  for (const value of values) {
    if (!target.includes(value)) {
      target.push(value);
    }
  }
}

const IMPLEMENTATION_PATTERNS = [
  /구현|수정|고쳐|바꿔|추가|삭제|만들|생성|적용|반영|리팩터|리팩토|정리|개선|마이그레이션|테스트|검증|계획|리뷰/u,
  /\b(implement|fix|change|update|add|remove|delete|create|build|refactor|clean\s*up|migrate|test|verify|plan|review)\b/i,
];

const REPOSITORY_GROUNDED_ANSWER_PATTERNS = [
  /왜|이유|근거|문서|내\s*문서|리포지토리|레포|repo|저장소|AGENTS|설계\s*문서|규칙|맞아\?|맞는/u,
  /\b(why|reason|rationale|basis|justify|justification|ground|evidence|docs?|document|repository|repo|rule|policy|architecture|design|decision|correct)\b/i,
];

const CATEGORY_RULES = [
  {
    label: "repository overview/tooling",
    patterns: [/AGENTS\.md|\.codex|hook|UserPromptSubmit|workspace|repo|저장소|프로젝트\s*설정|훅/u],
    docs: ["README.md", "ARCHITECTURE.md"],
  },
  {
    label: "backend",
    patterns: [/backend|백엔드|Spring|Java|Gradle|API|SSE|controller|service|repository|domain|feature\/|architectureLint/u],
    docs: [
      "BACKEND.md",
      "docs/design-docs/backend-design/README.md",
      "docs/design-docs/backend-design/01-architecture-foundations.md",
      "docs/design-docs/backend-design/02-package-and-wiring.md",
      "docs/design-docs/backend-design/03-application-and-providers.md",
    ],
  },
  {
    label: "backend domain/policy/calculation",
    patterns: [/도메인|정책|상태\s*전이|계산|공식|청산|liquidation|leverage|margin|order|position|PnL|수수료|잔고/u],
    docs: [
      "BACKEND.md",
      "docs/product-specs/README.md",
      "docs/product-specs/coin-futures-simulation-rules.md",
      "docs/design-docs/backend-design/04-domain-modeling-rules.md",
      "docs/design-docs/backend-design/03-application-and-providers.md",
    ],
  },
  {
    label: "backend persistence/db/external/exception",
    patterns: [/DB|schema|스키마|migration|Flyway|JPA|QueryDSL|Redis|MySQL|H2|entity|테이블|영속|예외|exception|CoreException|ErrorType|@?Modifying|dirty\s*checking|bulk\s*update|bulk|batch|JPQL|flushAutomatically|clearAutomatically/u],
    docs: [
      "BACKEND.md",
      "docs/design-docs/backend-design/06-persistence-rules.md",
      "docs/design-docs/backend-design/08-external-integration-rules.md",
      "docs/design-docs/backend-design/09-exception-rules.md",
      "docs/generated/db-schema.md",
    ],
  },
  {
    label: "Bitget/market data",
    patterns: [/Bitget|market\s*data|시장\s*데이터|candle|ticker|websocket|\bws\b|connector|외부\s*연동/u],
    docs: ["docs/references/README.md", "docs/references/bitget/"],
  },
  {
    label: "backend cleanup/responsibility",
    patterns: [/cleanup|deslop|clean\s*code|responsibility|책임|구조\s*정리|리팩터|리팩토/u],
    docs: [
      "BACKEND.md",
      "docs/design-docs/backend-design/07-clean-code-responsibility.md",
    ],
  },
  {
    label: "frontend/ui/state",
    patterns: [/frontend|프론트|Next|React|Tailwind|UI|화면|페이지|컴포넌트|component|route|hook|Zustand|React Query|브라우저/u],
    docs: [
      "FRONTEND.md",
      "frontend/README.md",
      "docs/design-docs/ui-design/README.md",
    ],
  },
  {
    label: "product/spec/user flow",
    patterns: [/제품|명세|스펙|시나리오|사용자\s*흐름|user\s*flow|screen|MVP|선물|거래/u],
    docs: [
      "docs/product-specs/README.md",
      "docs/product-specs/coin-futures-platform-mvp.md",
      "docs/product-specs/coin-futures-screen-spec.md",
      "docs/product-specs/coin-futures-simulation-rules.md",
      "docs/product-specs/coin-futures-candle-timeframe-spec.md",
    ],
  },
  {
    label: "release/ci",
    patterns: [/release|deploy|rollback|릴리즈|배포|롤백|CI|GitHub Actions|workflow/u],
    docs: ["RELEASE.md", "docs/release-docs/README.md", ".github/workflows/ci.yml"],
  },
  {
    label: "observability",
    patterns: [/observability|metric|metrics|log|dashboard|alert|monitor|관측|메트릭|로그|대시보드|알림|Sentry/u],
    docs: ["OBSERVABILITY.md", "docs/release-docs/observability/"],
  },
  {
    label: "exec plans/history",
    patterns: [/계획|plan|ralplan|과거\s*결정|decision|완료된\s*계획|exec/u],
    docs: ["docs/exec-plans/README.md"],
  },
];

function classifyDocs(prompt) {
  const docs = ["AGENTS.md"];
  const labels = [];
  for (const rule of CATEGORY_RULES) {
    if (hasAny(prompt, rule.patterns)) {
      labels.push(rule.label);
      addUnique(docs, rule.docs);
    }
  }
  if (docs.length === 1) {
    labels.push("repository overview");
    addUnique(docs, ["README.md", "ARCHITECTURE.md"]);
  }
  return { docs, labels };
}

function formatDocLine(root, doc) {
  const fullPath = path.join(root, doc);
  const exists = fs.existsSync(fullPath);
  return exists
    ? `- ${doc}`
    : `- ${doc} (missing in worktree; reconcile AGENTS.md or the docs index before relying on this path)`;
}

function buildContext(root, prompt) {
  const { docs, labels } = classifyDocs(prompt);
  const docLines = docs.map((doc) => formatDocLine(root, doc)).join("\n");
  const scope = labels.join(", ");

  return [
    "[coin-zzickmock project document gate]",
    "Before planning, implementing, editing files, running implementation-focused tools, or explaining/evaluating/justifying repository code/design decisions in this repository, you MUST read the project guidance documents selected from AGENTS.md.",
    "Do not make code or documentation edits until these reads are complete. If the scope changes while working, read the additional AGENTS.md-mapped documents before continuing.",
    "For repository-grounded questions, use repository documents and current code as the primary source. Use external sources only as explicitly-labeled supporting context after repo evidence is checked.",
    "In the final response, briefly state which project documents were read for this turn.",
    "",
    `Matched scope: ${scope}`,
    "Required documents for this prompt:",
    docLines,
  ].join("\n");
}

function main() {
  const payload = safeJsonParse(readStdin());
  const hookEventName = firstText(
    payload.hook_event_name,
    payload.hookEventName,
    payload.event,
    payload.name,
  );

  if (hookEventName && hookEventName !== "UserPromptSubmit") {
    process.stdout.write("{}");
    return;
  }

  const root = repoRoot(payload);
  const agentsPath = path.join(root, "AGENTS.md");
  let agentsText = "";
  try {
    agentsText = fs.readFileSync(agentsPath, "utf8");
  } catch {
    process.stdout.write(JSON.stringify({
      decision: "block",
      reason: "coin-zzickmock project document gate could not read AGENTS.md; inspect project guidance before continuing.",
      hookSpecificOutput: {
        hookEventName: "UserPromptSubmit",
        additionalContext: "AGENTS.md is required before implementation in this repository, but the project-local hook could not find it.",
      },
    }));
    return;
  }

  if (!agentsText.includes("## 먼저 읽을 문서")) {
    process.stdout.write(JSON.stringify({
      decision: "block",
      reason: "coin-zzickmock project document gate read AGENTS.md but could not find the required document index section.",
      hookSpecificOutput: {
        hookEventName: "UserPromptSubmit",
        additionalContext: "AGENTS.md must contain the `## 먼저 읽을 문서` document index so this hook can enforce project-specific pre-implementation reading.",
      },
    }));
    return;
  }

  const prompt = firstText(payload.prompt, payload.user_prompt, payload.userPrompt);
  if (!hasAny(prompt, IMPLEMENTATION_PATTERNS) && !hasAny(prompt, REPOSITORY_GROUNDED_ANSWER_PATTERNS)) {
    process.stdout.write("{}");
    return;
  }

  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: "UserPromptSubmit",
      additionalContext: buildContext(root, prompt),
    },
  }));
}

main();
