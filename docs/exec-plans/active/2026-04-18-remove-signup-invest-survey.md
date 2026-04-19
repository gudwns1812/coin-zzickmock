# 회원가입 투자성향 설문 제거

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서 하나만 읽어도 초보자가 "회원가입 직후 뜨는 투자성향 설문이 왜 나타나는지", "어디를 바꿔야 다시는 자동 노출되지 않는지", "무엇으로 검증해야 하는지"를 이해할 수 있게 유지한다.

## 목적 / 큰 그림

현재 프론트는 루트 레이아웃에서 `InvestSurveyProvider`를 전역으로 감싸고 있어, 로그인 또는 회원가입 직후 JWT의 `invest` 값이 `0`이면 어느 화면으로 들어가든 투자성향 설문 모달이 자동으로 열린다.
사용자가 요청한 것은 이 가입 후 설문 경험을 완전히 없애는 것이다.

이 작업이 끝나면 회원가입 직후 `/markets`로 이동해도 투자성향 설문 모달이 자동으로 뜨지 않아야 한다.
추가로 현재 코인 메인 플로우에서 더 이상 쓰이지 않는 전역 설문 잔재는 정리해, 다시 비슷한 회귀가 생기지 않도록 한다.

## 진행 현황

- [x] (2026-04-18 00:36+09:00) 요청 분석 완료: 전역 설문 노출 경로와 관련 문서 확인
- [x] (2026-04-18 00:41+09:00) 사용자 승인 완료
- [x] (2026-04-18 00:48+09:00) `red` 단계 완료: `frontend/app/layout.test.ts` 추가 후 전역 설문 provider 존재로 실패 확인
- [x] (2026-04-18 00:51+09:00) `green` 단계 완료: `frontend/app/layout.tsx` 에서 `InvestSurveyProvider` 와 `getJwtToken` 의존성 제거
- [x] (2026-04-18 00:52+09:00) `refactor` 단계 완료: 미사용 설문 컴포넌트 4개 삭제, `frontend/package.json` 테스트 스크립트 추가, 회원가입 메타데이터 문구 보정
- [x] (2026-04-18 00:56+09:00) 정적 검증 완료: `npm run test --workspace frontend`, `npm run lint --workspace frontend` 통과
- [x] (2026-04-18 01:02+09:00) 런타임 검증 시도 완료: `npm run dev --workspace frontend -- --port 3100` 기동과 포트 리슨 확인, 하지만 세션 네트워크 제약으로 `curl`/브라우저 자동화 접속은 실패
- [ ] 공식 품질 게이트: `multi-angle-review` 실행 필요
- [ ] 브랜치/커밋/push/PR 생성

## 놀라움과 발견

- 관찰:
  현재 코인 메인 포트폴리오 페이지는 이미 새 대시보드 구조로 바뀌어 있고, 투자성향 설문 관련 컴포넌트는 실사용 경로가 아니라 전역 레이아웃 잔재로 남아 있다.
  증거:
  [frontend/app/(main)/portfolio/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/portfolio/page.tsx) 는 `GaugeChart`, `CheckPortfolio`를 사용하지 않고, [frontend/app/layout.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/layout.tsx) 가 `InvestSurveyProvider`를 전역으로 감싼다.

- 관찰:
  프론트 개발 서버는 `lsof -iTCP:3100 -sTCP:LISTEN` 기준으로 정상 리슨했지만, 같은 세션의 `curl http://127.0.0.1:3100/signup` 와 Chrome DevTools 연결은 모두 실패했다.
  증거:
  `lsof` 출력에는 `node ... TCP *:3100 (LISTEN)` 이 있었고, `curl` 은 `Failed to connect` 로 종료됐다. Chrome DevTools는 기존 브라우저 프로필 충돌 메시지를 반환했다.

## 의사결정 기록

- 결정:
  이번 변경은 "회원가입 후 자동 설문 차단"을 넘어서 전역 투자성향 설문 기능 자체를 제거하는 범위로 진행한다.
  근거:
  사용자 표현이 "그거 다 없애버려"였고, 현재 실사용 경로에서는 설문 관련 코드가 코인 메인 흐름과 분리된 잔재에 가깝다.
  자동 팝업만 끊고 코드를 남겨두면 비슷한 회귀가 다시 생기기 쉽다.
  날짜/작성자:
  2026-04-18 / Codex

- 결정:
  테스트 인프라가 없는 현재 프론트에서는 Node 내장 `node:test` 로 `RootLayout` 소스 회귀 테스트를 추가해 red-green 근거를 남긴다.
  근거:
  React Testing Library나 Vitest가 없는 상태에서 새 의존성을 들이지 않고도 "전역 설문 wiring 제거"라는 이번 변경 핵심을 가장 직접적으로 고정할 수 있었다.
  날짜/작성자:
  2026-04-18 / Codex

## 결과 및 회고

- 회원가입 직후 자동으로 열리던 투자성향 설문은 `frontend/app/layout.tsx` 의 전역 wrapper 제거로 진입 경로 자체가 사라졌다.
- 더 이상 쓰이지 않던 투자성향 설문/가이드 컴포넌트 4개를 삭제해 실사용 코인 플로우와 레거시 주식 설문 잔재를 분리했다.
- `npm run test --workspace frontend`, `npm run lint --workspace frontend` 는 통과했다.
- 다만 저장소 기준의 공식 품질 게이트인 `multi-angle-review` 는 이 세션에서 sub-agent 사용 제약 때문에 아직 실행하지 못했고, 브라우저 자동화도 로컬 Chrome 프로필 충돌 및 세션 네트워크 제약으로 충분히 재현하지 못했다.
- 따라서 현재 상태는 "코드 변경과 기본 검증 완료, 공식 품질 게이트/PR 단계는 blocker" 로 기록한다.

## 맥락과 길잡이

관련 문서:

- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)

관련 코드:

- [frontend/app/layout.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/layout.tsx)
- [frontend/app/signup/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/signup/page.tsx)
- [frontend/components/router/InvestSurveyProvider.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/InvestSurveyProvider.tsx)
- [frontend/components/router/portfolio/GaugeChart.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/portfolio/GaugeChart.tsx)
- [frontend/components/router/portfolio/InvestmentStyleModal.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/portfolio/InvestmentStyleModal.tsx)
- [frontend/components/router/portfolio/CheckPortfolio.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/portfolio/CheckPortfolio.tsx)

현재 상태 설명:

- `frontend/app/layout.tsx` 는 `getJwtToken()`으로 토큰을 읽고, 그 토큰을 `InvestSurveyProvider`에 전달한다.
- `InvestSurveyProvider`는 `token?.invest === 0`이면 닫을 수 없는 모달을 전역으로 연다.
- 회원가입 성공 시 [frontend/components/router/siginup/RegisterStep2.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/siginup/RegisterStep2.tsx) 가 자동 로그인 후 `window.location.href = "/markets"`로 이동하므로, 새 토큰이 곧바로 전역 provider에 전달된다.
- 따라서 자동 설문 제거의 핵심은 회원가입 페이지가 아니라 루트 레이아웃 전역 감싸기를 제거하는 것이다.

## 작업 계획

먼저 `red` 단계에서 루트 레이아웃 소스가 `InvestSurveyProvider`와 `getJwtToken`에 더 이상 의존하지 않아야 한다는 회귀 테스트를 추가한다.
현재 상태에서는 이 테스트가 실패해야 하며, 자동 설문 노출 경로가 아직 살아 있음을 고정한다.

그 다음 `green` 단계에서 [frontend/app/layout.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/layout.tsx) 에서 `InvestSurveyProvider` import와 감싸기를 제거하고, 설문을 위해서만 쓰던 `getJwtToken()` 의존성도 함께 걷어낸다.
이렇게 하면 회원가입 직후 `/markets`로 이동하더라도 전역 설문이 열릴 경로가 사라진다.

마지막 `refactor` 단계에서는 더 이상 참조되지 않는 투자성향 설문 관련 컴포넌트 파일을 정리하고, 회원가입 레이아웃처럼 남아 있는 "주식 찍먹" 표현을 현재 코인 서비스 문맥에 맞게 최소 보정한다.

## 구체적인 단계

1. `frontend/app/layout.test.ts` 회귀 테스트를 추가한다.
2. `node --test frontend/app/layout.test.ts` 를 실행해 실패를 확인한다.
3. `frontend/app/layout.tsx` 에서 설문 provider 의존성을 제거한다.
4. 미사용 설문 컴포넌트 참조 여부를 다시 검색하고, 안전하게 제거 가능한 파일을 정리한다.
5. `npm run test --workspace frontend`, `npm run lint --workspace frontend` 를 실행한다.
6. 가능하면 프론트 개발 서버를 띄워 회원가입 후 `/markets` 진입 시 설문이 뜨지 않는지 확인한다.
7. 변경 범위만 대상으로 품질 게이트를 수행하고 PR을 만든다.

## 검증과 수용 기준

실행 명령:

- `node --test frontend/app/layout.test.ts`
- `npm run lint --workspace frontend`
- 가능하면 `npm run dev --workspace frontend` 후 회원가입/로그인 플로우 수동 확인

수용 기준:

- 루트 레이아웃 소스에 `InvestSurveyProvider` import 또는 JSX 감싸기가 없어야 한다.
- 회원가입 직후 `/markets`로 이동해도 투자성향 설문이 자동으로 열리지 않아야 한다.
- 설문 제거로 인해 프론트 타입체크가 깨지지 않아야 한다.
- 브라우저 자동화나 실제 회원가입 API가 막혀 있으면, 최소한 전역 설문 wiring 제거 테스트와 타입체크 통과를 남기고 blocker를 문서화해야 한다.

## 반복 실행 가능성 및 복구

- 회귀 테스트와 타입체크는 여러 번 반복 실행해도 안전하다.
- 설문 컴포넌트 제거 후 다른 파일에서 참조 에러가 나면 `rg` 검색 결과를 기준으로 삭제 범위를 줄이고 다시 타입체크한다.

## 산출물과 메모

- 기대 산출물은 `frontend/app/layout.tsx`의 전역 설문 제거 diff와 이를 고정하는 회귀 테스트다.

## 인터페이스와 의존성

- 새 외부 라이브러리는 추가하지 않는다.
- 테스트는 Node 내장 `node:test`를 사용해 현재 저장소 의존성만으로 실행 가능하게 유지한다.

변경 메모:

- 2026-04-18: 사용자 요청에 따라 회원가입 직후 자동 노출되는 투자성향 설문 제거 계획을 새로 작성했다.
- 2026-04-18: red/green/refactor, 정적 검증, 런타임 검증 제약, 품질 게이트 blocker 상태를 반영해 문서를 갱신했다.
