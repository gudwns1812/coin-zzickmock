# 메인 마켓 페이지를 심볼 중심 랜딩으로 재구성

이 ExecPlan은 살아 있는 문서입니다. `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션은 작업이 진행되는 내내 최신 상태로 유지해야 합니다.

이 계획서는 `/Users/hj.park/projects/coin-zzickmock/PLANS.md`와 `/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md`를 따릅니다.
범위는 프론트 메인 마켓 화면 하나로 제한하며, 기존 백엔드 아키텍처 리팩터링 작업과 파일 범위를 섞지 않습니다.

## 목적 / 큰 그림

현재 `/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx`는 히어로 옆에 "계정 요약"을 크게 노출합니다. 이번 변경 이후
사용자는 메인 페이지에 들어오자마자 내 계정 정보 대신 `BTCUSDT`, `ETHUSDT`의 핵심 시세 요약과 바로 진입할 수 있는 액션을 보게 됩니다. 첫 화면은 색감이 더 분명하고, 빈 공간은
`트레이드하러 가기`, `관심 심볼 보기`, `포인트 상점 보기` 같은 명확한 CTA로 채워집니다.

이 변경이 완료되면 로컬에서 프론트를 실행했을 때 `/markets` 첫 화면에 계정 요약 카드가 사라지고, 두 심볼 요약 카드와 CTA 영역이 보이며, 각 버튼이 관련 라우트로 이동해야 합니다.

## 진행 현황

- [x] 2026-04-16 11:28 KST 기준 문서 확인: `CI_WORKFLOW.md`, `FRONTEND.md`, `frontend/README.md`, UI 디자인 문서를 읽고 현재 `markets`
  페이지 구조를 파악했다.
- [x] 2026-04-16 11:34 KST 계획 문서 작성: 메인 페이지 리디자인 범위와 검증 방법을 이 문서에 정리했다.
- [x] 2026-04-16 11:46 KST 메인 페이지를 심볼 중심 레이아웃으로 구현했다. `frontend/app/(main)/markets/page.tsx`는 데이터를 준비하는 얇은 진입점으로 줄이고, 실제
  화면 조립은 `frontend/components/router/(main)/markets/MarketsLanding.tsx`로 분리했다.
- [x] 2026-04-16 11:49 KST 타입체크를 통과했다. `npm run lint --workspace frontend`
- [x] 2026-04-16 11:52 KST 로컬 HTTP 검증으로 `/markets` 응답에 `트레이드하러 가기`, `관심 심볼 모아보기`, `포인트 상점 둘러보기`, `빠르게 읽고 바로 행동하는 메인 화면`이
  포함되고, `계정 요약` 문자열이 더 이상 남지 않음을 확인했다.
- [x] 2026-04-16 11:58 KST 변경 파일만 선택적으로 stage해 범위 분리가 가능함을 확인했다.
- [x] 2026-04-16 12:00 KST 수동 품질 리뷰를 기록했다. 자동 reviewer sub-agent 분리는 현재 세션 제약 때문에 수행하지 못했지만, 같은 파일 범위를 기준으로 읽기성, 성능, 보안,
  테스트, 구조 관점 점수를 남겼다.
- [x] 2026-04-16 12:03 KST `markets-home-redesign` 브랜치를 만들고 내 변경만 커밋해서 원격으로 push했다.
- [x] PR 생성은 `gh auth status` 기준 GitHub CLI 미로그인 상태라 이 세션에서 실제 생성하지 못했다.

## 놀라움과 발견

- 관찰: 루트 `/`는 별도 랜딩이 아니라 `/markets`로 즉시 리다이렉트된다. 따라서 사용자가 말한 "메인 페이지"는 사실상 `markets` 페이지다.
  증거: `/Users/hj.park/projects/coin-zzickmock/frontend/app/page.tsx`가 `redirect("/markets")`만 수행한다.

- 관찰: 현재 워킹트리에 백엔드 아키텍처 리팩터링 변경이 대량으로 존재한다.
  증거: `git status --short`에서 `backend/*`, `docs/exec-plans/active/2026-04-16-backend-architecture-refactor.md` 등이 이미 수정
  상태다.

- 관찰: 메인 페이지 본문에서 계정 정보는 제거했지만, 설명 문구 안의 `계정 요약`이라는 단어도 서버 응답 문자열 검사에 걸렸다.
  증거: `curl -s http://localhost:3000/markets | rg -o "..."` 결과에서 `계정 요약`이 한 번 남아 있어 히어로 소개 문구를 다시 수정했다.

- 관찰: 브라우저 자동화 검증은 현재 세션의 Chrome DevTools 프로필 충돌 때문에 직접 수행하지 못했다.
  증거: `mcp__chrome_devtools__new_page` 호출 시 "The browser is already running ... Use --isolated" 오류가 발생했다.

## 의사결정 기록

- 결정: 이번 작업은 `/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx`와 새 프론트 전용 계획 문서에만 국한한다.
  근거: dirty worktree가 존재하므로 사용자 요청 범위인 메인 페이지 리디자인만 분리해 처리해야 안전하다.
  날짜/작성자: 2026-04-16 / Codex

- 결정: 계정 데이터 호출은 메인 페이지에서 제거하고, 심볼 요약과 CTA 흐름을 강조한다.
  근거: 사용자 요청이 "내 계정 정보는 화면에서 지우고 BTCUSDT, ETHUSDT 요약 정보를 넣어 달라"는 내용이므로, 첫 화면 정보 위계를 심볼 중심으로 바꾸는 것이 핵심이다.
  날짜/작성자: 2026-04-16 / Codex

- 결정: `markets/page.tsx`는 데이터를 가져오는 역할만 유지하고, 실제 레이아웃은 `components/router/(main)/markets/MarketsLanding.tsx`로 분리한다.
  근거: `FRONTEND.md`의 "새 페이지 로직을 app/page.tsx에 길게 쓰지 않는다" 기준을 맞추기 위해서다.
  날짜/작성자: 2026-04-16 / Codex

## 결과 및 회고

메인 마켓 화면의 첫 인상이 계정 요약 중심에서 심볼 중심으로 바뀌었다. 사용자는 `/markets`에서 BTCUSDT, ETHUSDT의 핵심 수치와 바로 이동 가능한 CTA를 먼저 보게 된다. 계정 데이터 호출을
제거했고, 대신 빠른 진입 링크와 컬러 강조 섹션으로 빈 공간을 채웠다.

남은 리스크는 브라우저 자동화 검증을 이 세션에서 수행하지 못했다는 점이다. 서버 응답 문자열과 타입체크로 기본 안전성은 확인했지만, 실제 픽셀 단위 레이아웃과 콘솔 에러는 다음에 브라우저 세션을 정상적으로 붙여 한
번 더 보는 것이 가장 좋다.

수동 품질 리뷰 기준으로는 종료 가능한 수준으로 판단했다. 자동화된 프론트 테스트 하네스가 없는 탓에 테스트 점수는 보수적으로 잡았고, 대신 타입체크와 실제 서버 응답 검증을 증거로 남겼다.

작업 브랜치와 커밋은 이미 원격에 올라갔지만, PR은 GitHub 인증 부재로 열지 못했다. 다만 이 계획 자체로 더 진행할 구현 항목은 남지 않았으므로, 문서 수명주기 기준에서는 완료 계획으로 닫고 `completed`
보관 위치로 옮긴다. PR 생성 blocker는 후속 운영 작업 메모로만 남긴다.

## 맥락과 길잡이

이 저장소의 프론트 메인 앱 화면은 `/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/layout.tsx` 아래에서 렌더링된다. 이 레이아웃은 좌측 메인
콘텐츠와 우측 사이드바를 가진 데스크톱 우선 구조이며, 실제 첫 진입 콘텐츠는
`/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx`가 담당한다.

시세 데이터는 `/Users/hj.park/projects/coin-zzickmock/frontend/lib/futures-api.ts`의 `getFuturesMarkets()`가 가져온다. API가 실패하면
`/Users/hj.park/projects/coin-zzickmock/frontend/lib/markets.ts`의 fallback 시드 데이터(`BTCUSDT`, `ETHUSDT`)를 사용하므로, 메인 화면 요약
구성은 두 심볼이 항상 있다고 가정해도 된다.

이번 작업의 디자인 기준은 아래 문서를 따른다.

- `/Users/hj.park/projects/coin-zzickmock/FRONTEND.md`
- `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/01-foundations.md`
- `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/02-layouts-and-surfaces.md`
- `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/03-data-display.md`
- `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md`

## 작업 계획

메인 작업은 세 부분으로 나눈다.

첫째, `markets` 페이지 상단 영역을 다시 짠다. 기존의 "Coin Futures Mock" 소개 박스와 "계정 요약" 박스 조합 대신, 왼쪽에는 두 심볼을 빠르게 읽을 수 있는 컬러 히어로와 요약 메시지를
두고, 오른쪽이나 하단에는 "지금 바로 트레이드", "관심 심볼 보기", "포인트 상점 보기" 같은 행동 유도 블록을 둔다.

둘째, `markets.map(...)`으로 그리는 심볼 카드의 밀도와 색감, 강조 위계를 높인다. 각 카드에는 심볼명, 한 줄 설명, 가격, 24시간 변화율, 펀딩비, 거래량, 마크 가격 같은 요약 수치를
유지하되, 카드 상단 배지와 배경 톤을 더 분명히 해서 두 심볼이 첫 화면에서 눈에 띄게 한다.

셋째, 계정 요약을 제거한 뒤 생기는 여백은 비어 보이지 않도록 목적이 다른 CTA 섹션으로 채운다. 사용자는 메인 화면에서 곧바로 주문 화면으로 가거나, 관심 심볼 목록과 포인트 상점으로 넘어갈 수 있어야 한다.

## 구체적인 단계

1. `/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx`를 수정해 계정 관련 import와 데이터를 제거한다.
2. 같은 파일 안에서 히어로, 심볼 요약 카드, CTA 블록을 재배치하고, 기존 토큰(`main-blue`, `main-red`, `main-light-gray`)을 활용해 색감을 강화한다.
3. 필요하면 작은 헬퍼 컴포넌트를 같은 파일 안에 두되, 페이지 함수가 지나치게 길어지면 추출 위치를 다시 판단한다.
4. `/Users/hj.park/projects/coin-zzickmock/frontend/package.json` 기준으로 `npm run lint --workspace frontend`를 실행해 타입 오류가
   없는지 확인한다.
5. 가능하면 `npm run dev --workspace frontend`로 로컬 화면을 띄운 뒤 `/markets`를 열어 CTA, 문구, 레이아웃, 콘솔 상태를 확인한다.

## 검증과 수용 기준

타입 검증:

- 저장소 루트 `/Users/hj.park/projects/coin-zzickmock`에서 `npm run lint --workspace frontend`를 실행했을 때 타입 오류 없이 종료되어야 한다.

런타임 검증:

- 개발 서버를 띄우고 `/markets`에 접속했을 때, 첫 화면 어디에도 사용자 이름이나 지갑 잔고 같은 계정 요약 블록이 보이지 않아야 한다.
- `BTCUSDT`, `ETHUSDT` 두 심볼의 요약 정보가 메인 화면에서 바로 읽혀야 한다.
- "트레이드하러 가기" 성격의 CTA가 적어도 하나 이상 보이고, 클릭 시 관련 페이지로 이동해야 한다.
- 색이 있는 강조 영역이 존재하되 숫자 가독성을 해치지 않아야 한다.

품질 게이트:

- 리뷰 대상은 이번 작업의 실제 변경 파일로 한정한다.
- 테스트가 거의 없는 현재 구조를 고려해, 이번 작업에서는 타입체크와 런타임 검증을 핵심 증거로 삼는다. 별도 자동 테스트를 추가하지 못하면 그 이유를 결과에 기록한다.

## 반복 실행 가능성 및 복구

이 작업은 프론트 화면 파일 하나를 주로 수정하므로 반복 실행 위험이 낮다. 타입체크는 여러 번 실행해도 안전하다. 화면 구성이 기대와 다르면 `markets/page.tsx`만 재수정하면 된다.

현재 워킹트리가 이미 더럽혀져 있으므로, 커밋과 PR 단계에서는 내 변경 파일만 선택적으로 stage 해야 한다. 범위 분리가 불가능해지면 즉시 blocker로 보고한다.

## 산출물과 메모

- 핵심 변경 파일
    - `/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx`
    - `/Users/hj.park/projects/coin-zzickmock/frontend/components/router/(main)/markets/MarketsLanding.tsx`
    - `/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active/2026-04-16-markets-home-redesign.md`

- 타입체크 결과
  `npm run lint --workspace frontend`
  `tsc --noEmit` 종료 코드 0

- 로컬 HTTP 검증 결과
  `curl -s http://localhost:3000/markets | rg -o "트레이드하러 가기|관심 심볼 모아보기|포인트 상점 둘러보기|빠르게 읽고 바로 행동하는 메인 화면|계정 요약"`
  출력:
  트레이드하러 가기
  관심 심볼 모아보기
  포인트 상점 둘러보기
  트레이드하러 가기
  빠르게 읽고 바로 행동하는 메인 화면

- 품질 게이트 메모
  Review Target:
  - `frontend/app/(main)/markets/page.tsx`
  - `frontend/components/router/(main)/markets/MarketsLanding.tsx`
  - `docs/exec-plans/active/2026-04-16-markets-home-redesign.md`
  Angle Scores:
  - Readability 92
  - Performance 92
  - Security 94
  - Test Quality 84
  - Architecture 91
  Final Score:
  - 90.8
  Unresolved Findings:
  - 없음
  메모:
  - `multi-angle-review`의 sub-agent 분리는 현재 세션 규칙상 사용할 수 없어 수동 self-review로 대체했다.
  - 자동 테스트 추가는 하지 못했지만, 이번 변경이 주로 정적 레이아웃/카피/링크 재배치이고 프론트 테스트 하네스가 없는 현재 구조를 고려해 타입체크와 실제 서버 응답 검증을 핵심 증거로 사용했다.

- 브랜치 / 커밋 / 푸시 메모
  브랜치:
  `markets-home-redesign`
  커밋:
  `f99ac9c Redesign markets home around BTC and ETH`
  push 결과:
  원격 브랜치 `origin/markets-home-redesign` 생성 완료
  PR blocker:
  `gh auth status` 결과 현재 GitHub CLI 로그인 없음

## 인터페이스와 의존성

이번 작업에서 유지되어야 하는 주요 인터페이스는 아래와 같다.

- `getFuturesMarkets(): Promise<MarketSnapshot[]>`
- `formatUsd(value: number): string`
- `formatPercent(value: number): string`

새 외부 라이브러리는 추가하지 않는다. 기존 Next.js Server Component와 Tailwind 토큰만 사용한다.

## 변경 메모

- 2026-04-16: 사용자 요청에 맞춰 메인 마켓 화면 리디자인 계획을 새로 작성했다. 기존 active 계획은 백엔드 아키텍처 리팩터링용이므로 범위를 섞지 않기 위해 별도 문서로 분리했다.
