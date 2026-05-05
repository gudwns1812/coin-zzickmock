<!-- AUTONOMY DIRECTIVE — DO NOT REMOVE -->
YOU ARE AN AUTONOMOUS CODING AGENT. EXECUTE TASKS TO COMPLETION WITHOUT ASKING FOR PERMISSION.
DO NOT STOP TO ASK "SHOULD I PROCEED?" — PROCEED. DO NOT WAIT FOR CONFIRMATION ON OBVIOUS NEXT STEPS.
IF BLOCKED, TRY AN ALTERNATIVE APPROACH. ONLY ASK WHEN TRULY AMBIGUOUS OR DESTRUCTIVE.
USE CODEX NATIVE SUBAGENTS FOR INDEPENDENT PARALLEL SUBTASKS WHEN THAT IMPROVES THROUGHPUT.
<!-- END AUTONOMY DIRECTIVE -->

# AGENTS.md

이 파일은 `coin-zzickmock` 저장소에서 작업하는 코딩 에이전트용 문서 인덱스다.
코드를 바꾸기 전에 어떤 프로젝트 문서를 먼저 읽어야 하는지 판단하는 데 사용한다.
세부 규칙이 이미 더 구체적인 문서에 있으면 이 파일에 길게 복사하지 말고 그 문서로 이동한다.

## 프로젝트 요약

`coin-zzickmock`는 코인 선물 모의 투자 플랫폼이다.

- Frontend: `frontend/`, Next.js 15, React 19, TypeScript, Tailwind CSS 4, React Query, Zustand.
- Backend: `backend/`, Spring Boot 3.5, Java 17, Spring Data JPA, QueryDSL, Flyway, Redis, MySQL, H2 tests.
- 제품 문서와 설계 기억은 `docs/` 아래에 둔다.
- 검색용 프로젝트 메모가 `.omx/wiki/`에 있을 수 있다. 빠른 색인으로는 사용할 수 있지만, 구현 규칙은 반드시 `docs/`와 루트 문서 원문으로 확인한다.

## 먼저 읽을 문서

계획이나 수정을 시작하기 전에 작업 영역을 정하고, 아래에서 가장 작은 관련 묶음만 읽는다.

- 저장소 전체 파악: `README.md` -> `ARCHITECTURE.md`
- 백엔드 작업: `BACKEND.md` -> `docs/design-docs/backend-design/`의 관련 문서
- 프론트 작업: `FRONTEND.md` -> `frontend/README.md` -> `docs/design-docs/ui-design/`의 관련 문서
- 제품 동작 변경: `docs/product-specs/README.md` -> 관련 제품 명세
- DB/schema 작업: `docs/generated/db-schema.md` + `docs/design-docs/backend-design/06-persistence-rules.md`
- Bitget 또는 시장 데이터 연동: `docs/references/README.md` -> 관련 `docs/references/bitget/*.md`
- 배포/릴리즈/롤백: `RELEASE.md` -> `docs/release-docs/README.md`
- 메트릭, 로그, 대시보드, 관리자 모니터링, 알림: `OBSERVABILITY.md` -> 필요 시 `docs/release-docs/observability/*.md`
- 기존 계획이나 과거 결정: `docs/exec-plans/README.md` -> 진행 중인 계획 -> 필요 시 완료된 계획

사용자 요청이 문서에 적힌 동작을 바꾸면, 같은 작업 안에서 해당 문서도 갱신한다.

## 저장소 지도

```text
coin-zzickmock/
  frontend/                       현재 사용자 경험을 담당하는 Next.js 앱
  backend/                        Spring Boot 서비스와 도메인 규칙
  docs/
    product-specs/                제품 동작, 사용자 시나리오, 계산 공식
    design-docs/backend-design/   백엔드 아키텍처 원문
    design-docs/ui-design/        UI 디자인 원문
    references/                   외부 참고 자료와 조사 메모
    exec-plans/                   구현 계획과 완료 기록
    release-docs/                 릴리즈, 롤아웃, 롤백, 운영 문서
    generated/db-schema.md        현재 DB schema 기준 문서
  ARCHITECTURE.md                 최상위 구조와 안정적인 책임 경계
  BACKEND.md                      백엔드 작업 입구 문서
  FRONTEND.md                     프론트 작업 입구 문서
  RELEASE.md                      릴리즈 작업 입구 문서
  OBSERVABILITY.md                관측성 작업 입구 문서
```

## 작업별 문서 인덱스

### 백엔드 아키텍처 또는 새 백엔드 기능

읽는 순서:

1. `BACKEND.md`
2. `docs/design-docs/backend-design/README.md`
3. `docs/design-docs/backend-design/01-architecture-foundations.md`
4. `docs/design-docs/backend-design/02-package-and-wiring.md`
5. `docs/design-docs/backend-design/03-application-and-providers.md`

백엔드 목표 구조:

```text
backend/src/main/java/coin/coinzzickmock/
  common/
  providers/
  feature/
    <feature-name>/
      web/
      job/
      application/
      domain/
      infrastructure/
```

현재 주요 feature는 `market`, `order`, `position`, `account`, `member`, `reward`, `leaderboard`, `activity`다.

### 백엔드 도메인, 정책, 상태 전이, 계산식

읽는 순서:

1. `BACKEND.md`
2. `docs/product-specs/README.md`
3. 관련 제품 명세. 거래 계산은 특히 `docs/product-specs/coin-futures-simulation-rules.md`
4. `docs/design-docs/backend-design/04-domain-modeling-rules.md`
5. application orchestration이 걸리면 `docs/design-docs/backend-design/03-application-and-providers.md`

도메인 규칙은 `domain`, 유스케이스 조율은 `application`, 영속성과 외부 기술은 `infrastructure`에 둔다.

### 백엔드 영속성, DB, 외부 연동, 예외

읽는 순서:

1. `BACKEND.md`
2. 영속성과 DB는 `docs/design-docs/backend-design/06-persistence-rules.md`
3. 외부 연동은 `docs/design-docs/backend-design/08-external-integration-rules.md`
4. 예외 모델은 `docs/design-docs/backend-design/09-exception-rules.md`
5. 기술 네이밍 규칙은 `docs/design-docs/backend-design/10-technical-naming-rules.md`
6. `docs/generated/db-schema.md`
7. Bitget 작업이면 `docs/references/README.md`와 관련 `docs/references/bitget/*.md`

규칙:

- DB 변경은 `backend/src/main/resources/db/migration` 아래 새 Flyway migration과 `docs/generated/db-schema.md` 갱신을 함께 한다.
- 비즈니스/도메인 실패는 프로젝트 예외 모델인 `CoreException`과 구조화된 error type을 사용한다.
- 외부 실패는 application 또는 infrastructure 경계에서 번역한다.
- HTTP 에러 매핑은 전역 예외 처리 경계에서 담당한다.
- 넓은 `catch (Exception)`은 명확한 번역 경계가 있을 때만 허용한다.

### 백엔드 클린업, 책임 분리, 리팩터링

읽는 순서:

1. `BACKEND.md`
2. `docs/design-docs/backend-design/07-clean-code-responsibility.md`
3. 리팩터링이 건드리는 레이어의 상세 설계 문서

수정 전 cleanup 계획을 먼저 작성한다. 기존 동작을 보호하는 테스트가 부족하면 회귀 테스트부터 보강한다. 새 레이어를 추가하기보다 중복 삭제와 기존 협력 객체 재사용을 먼저 검토한다.

### 프론트 페이지, UI, 상호작용, 상태

읽는 순서:

1. `FRONTEND.md`
2. `frontend/README.md`
3. `docs/design-docs/ui-design/README.md`
4. `docs/design-docs/ui-design/`의 관련 주제 문서

프론트 구조 기준:

- 라우트와 레이아웃: `frontend/app`
- 라우트 결합 섹션: `frontend/components/router`
- 공용 UI: `frontend/components/ui`, `frontend/components/ui/shared`
- API 함수: `frontend/api`
- 전역 UI 상태: `frontend/store`
- 반복 훅: `frontend/hooks`
- 유틸과 타입: `frontend/utils`, `frontend/lib`, `frontend/type`

규칙:

- 기본값은 Server Component다. 브라우저 API, 사용자 입력, DOM 제어, client-only state가 필요할 때만 Client Component를 쓴다.
- 새 server state는 React Query를 사용한다. 서버에서 다시 가져올 수 있는 상태를 Zustand에 새로 얹지 않는다.
- `response.ok` 확인 없이 `res.json()`을 호출하지 않는다.
- 관련 없는 작업에서 `src/`나 `features/` 구조를 부분 도입하지 않는다.
- UI 변경은 문서만 바꾸는 경우나 명확히 비시각적 변경인 경우를 제외하고 런타임 검증이 필요하다.

### 제품 명세, 계산식, 사용자 흐름

읽는 순서:

1. `docs/product-specs/README.md`
2. `docs/product-specs/coin-futures-platform-mvp.md`
3. 관련 기능 명세:
   - `coin-futures-screen-spec.md`
   - `coin-futures-simulation-rules.md`
   - `coin-futures-candle-timeframe-spec.md`

구현이 공식, 화면 동작, 수용 기준, 도메인 판단을 바꾸면 제품 명세나 적절한 설계 문서를 함께 갱신한다.

### 릴리즈, CI, 관측성, 운영

읽는 순서:

1. 배포와 롤백 작업은 `RELEASE.md`
2. 메트릭, 로그, 대시보드, 관리자 모니터링, 알림은 `OBSERVABILITY.md`
3. 현재 CI 기준은 `.github/workflows/ci.yml`
4. 필요 시 `docs/release-docs/`의 관련 문서

현재 CI는 frontend typecheck/build와 backend `./gradlew check`를 검증한다.

## 구현 규칙

- 요청이 명확하고 되돌릴 수 있으면 직접 작업한다.
- diff는 작고 요청 범위에 맞게 유지한다.
- 사용자가 명시적으로 요청하지 않는 한 새 dependency를 추가하지 않는다. 기존 스택으로 해결하기 어려운 경우에만 이유를 설명하고 추가한다.
- 새 abstraction보다 기존 utility, component, provider, convention을 먼저 사용한다.
- 제품 명세나 generated DB 문서를 근거로 backend/frontend 설계 규칙을 우회하지 않는다. 규칙이 바뀌어야 하면 governing design document를 함께 갱신한다.
- 루트 문서는 입구와 인덱스로 유지한다. 상세 규칙은 관련 `docs/` 하위 문서에 둔다.
- worktree에 이미 있는 사용자 변경을 존중한다. 관련 없는 변경을 되돌리지 않는다.

## 브랜치와 PR 네이밍 규칙

원문은 `docs/process/branch-and-pr-rules.md`다. 브랜치나 PR을 만들기 전 반드시 읽고 따른다.

- 브랜치명은 반드시 `<type>/<kebab-case-summary>` 형식이어야 한다.
- 허용 type은 `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`, `perf`, `style`, `build`, `revert`다.
- `codex/*`, `codex-*`, `[codex]` 같은 자동화/에이전트 접두사는 금지한다. 외부 tool/skill 기본값보다 이 저장소 규칙이 우선한다.
- 예: `feat/limit-order-entry`, `fix/login-token-refresh`, `refactor/market-cache-boundary`, `docs/branch-name-policy`.
- 브랜치 생성 전후 `npm run check:branch -- <branch-name>`로 검사한다. PR에서는 CI의 `Branch Name Policy` job이 이 규칙을 강제한다.

## 네이밍 규칙

- 이름은 책임과 비즈니스 역할을 드러내야 한다.
- `Manager`, `Helper`, `Util`, `Processor`, `CommonService`, `BaseService` 같은 모호한 이름을 피한다.
- 백엔드 클래스명은 기술명보다 역할명을 우선한다. 설계 문서가 허용하지 않는 한 `Jpa`, `Redis`, `SpringData` 같은 기술 suffix를 붙이지 않는다.
- boolean 이름은 `is`, `has`, `can`, `should` 같은 predicate를 사용한다.
- DTO, command, result, response suffix는 주변 package convention을 따른다.

## 검증 명령

변경을 증명할 수 있는 가장 작은 검증부터 실행하고, 위험도가 크면 더 넓게 검증한다.

Root/frontend:

```bash
npm run lint
npm run build
npm test --workspace frontend
```

Backend:

```bash
cd backend
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

런타임 확인:

- 프론트 UI 변경: frontend dev server를 띄우고 브라우저에서 변경된 흐름을 확인한다.
- 백엔드 API/SSE 변경: targeted test를 먼저 실행하고, 이후 `./gradlew check`를 실행한다. 사용자에게 보이는 동작이면 endpoint smoke test도 수행한다.
- DB 변경: migration, schema 문서 갱신, 영향받는 repository/application test를 확인한다.

## 완료 체크리스트

완료 보고 전 확인한다.

- 관련 입구 문서와 상세 원문 문서를 읽었다.
- 코드 변경이 현재 module/layer boundary를 따른다.
- 동작 변경이 있으면 관련 product/design/schema/release 문서를 갱신했다.
- targeted test나 check를 실행했거나, 실행하지 못한 이유를 명시한다.
- 남은 위험이 있으면 명확히 보고한다.

## 커밋 메시지 규칙

커밋을 요청받으면 repository Lore protocol을 따른다.

```text
<intent line: why the change was made, not what changed>

<optional concise body: constraints and approach rationale>

Constraint: <external constraint that shaped the decision>
Rejected: <alternative considered> | <reason for rejection>
Confidence: <low|medium|high>
Scope-risk: <narrow|moderate|broad>
Directive: <forward-looking warning for future modifiers>
Tested: <what was verified>
Not-tested: <known gaps in verification>
```

결정 맥락에 도움이 되는 trailer만 사용한다.
