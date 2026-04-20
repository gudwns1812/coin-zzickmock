# FRONTEND.md

## Purpose

이 문서는 `coin-zzickmock` 프론트엔드 작업의 입구 문서다.
예전처럼 UI 상세 설계와 구현 규칙을 한 파일에 모두 쌓기보다, 작업 기준과 읽기 순서를 짧게 정리한다.

상세 설계와 구조 문서는 아래로 분리한다.

- [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
- [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
- [docs/design-docs/ui-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/README.md)
- [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)

## What This File Does

이 문서는 아래 역할만 맡는다.

- 프론트 작업 전에 무엇을 읽어야 하는지 알려 준다.
- 절대 놓치면 안 되는 구현 기준만 짧게 고정한다.
- UI 설계, 런타임 검증, 워크스페이스 codemap 문서를 연결한다.

즉, "프론트 작업용 체크인 문서"라고 보면 된다.

## Read Order

### 화면 구조나 새 UI를 설계할 때

1. [docs/design-docs/ui-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/README.md)
2. [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
3. 보안이 걸리면 [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

### 상태, 데이터 패칭, 컴포넌트 배치를 바꿀 때

1. [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
2. 이 문서의 Non-negotiables
3. UI 의미 체계가
   바뀌면 [docs/design-docs/ui-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/README.md)

### 프론트 검증 기준을 확인할 때

1. 이 문서의 `Runtime Verification`
2. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
3.
필요하면 [docs/design-docs/ui-design/05-motion-states-accessibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md)

## Current Reality

현재 프론트엔드는 다음 현실을 가진다.

- Next.js App Router를 사용한다.
- `src/` 없이 `frontend/` 루트 바로 아래에 `app`, `components`, `store`, `api`, `hooks`, `utils`, `type`가 있다.
- React Query와 Zustand가 둘 다 존재한다.
- 스타일은 Tailwind CSS 4 + CSS 변수 기반이다.
- 시각적으로는 데스크톱 우선 금융 대시보드 UI다.
- 이미 레거시 `components/router/*` 구조가 넓게 퍼져 있다.

중요:

- 현재 구조를 무시한 이상적인 샘플 구조를 강제하지 않는다.
- 대신 "이 구조 안에서 어떻게 더 망가지지 않게 만들 것인가"를 우선 규정한다.

## Non-negotiables

아래는 상세 문서로 내려가지 않더라도 기억해야 하는 핵심 규칙이다.

- 기본값은 Server Component다.
- 브라우저 기능, 사용자 입력, DOM 제어가 필요할 때만 Client Component를 쓴다.
- `response.ok` 확인 없이 `res.json()`을 호출하지 않는다.
- 새 서버 상태는 React Query로 다루고, Zustand에 새로 얹지 않는다.
- `useEffect` 안에서 fetch를 기본 패턴처럼 쓰지 않는다.
- `any`와 습관적 non-null assertion `!`을 새로 늘리지 않는다.
- 새 페이지 로직을 `app/page.tsx`에 길게 쓰지 않는다.
- fetch 코드를 컴포넌트 안에 직접 중복 작성하지 않는다.
- 공용 버튼, 인풋, 모달 스타일을 화면 안에서 다시 정의하지 않는다.
- 폼 검증 로직을 화면 곳곳에 흩뿌리지 않는다.
- 핵심 플로우 변경은 테스트 또는 런타임 검증 없이 끝내지 않는다.

## Structure Guardrails

- 라우트와 레이아웃: `frontend/app`
- 공용 UI: `frontend/components/ui`, `frontend/components/ui/shared`
- 라우트 결합 섹션: `frontend/components/router`
- API 함수: `frontend/api`
- 전역 UI 상태: `frontend/store`
- 반복 훅: `frontend/hooks`
- 프레임워크 비의존 유틸: `frontend/utils`
- 도메인 타입: `frontend/type`

강한 규칙:

- 새 기능을 넣으면서 `src/` 구조를 부분적으로 도입하지 않는다.
- `features/` 구조로 바꾸고 싶다면 기능 단위를 통째로 옮기는 별도 작업으로 진행한다.
- 공용 승격
  여부는 [docs/design-docs/ui-design/06-component-boundaries.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/06-component-boundaries.md)
  를 따른다.

## Data And State Guardrails

- 페이지 진입에 꼭 필요한 데이터는 서버에서 먼저 준비한다.
- 상호작용에 따라 바뀌는 데이터는 React Query를 사용한다.
- 상태 우선순위는 `URL state -> Server state -> Local state -> Global UI state`를 따른다.
- 기존 Zustand store를 수정할 때는 API 호출을 밖으로 빼는 방향을 먼저 검토한다.

## UI Design Source Of Truth

프론트의 시각적 상세 설계 원문은 `FRONTEND.md`가 아니라 `docs/design-docs/ui-design/`이다.

- 시각 언어와 토큰: [01-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/01-foundations.md)
- 레이아웃과
  표면: [02-layouts-and-surfaces.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/02-layouts-and-surfaces.md)
- 데이터 표시: [03-data-display.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/03-data-display.md)
- 입력과
  모달: [04-inputs-and-overlays.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/04-inputs-and-overlays.md)
- 모션과
  접근성: [05-motion-states-accessibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md)
- 컴포넌트
  경계: [06-component-boundaries.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/06-component-boundaries.md)

즉, UI 기준이 길어질수록 이 파일을 키우는 대신 `docs/design-docs/ui-design/`을 갱신한다.

## Runtime Verification

프론트 변경은 정적 코드 확인만으로 끝내지 않는다.
상호작용, 레이아웃, 시각 피드백이 바뀌는 경우에는 런타임에서 실제 UI 반응을 확인한다.
프론트엔드의 검증 기준은 `QUALITY_SCORE.md` 같은 점수 문서가 아니라, 사용자가 실제 화면에서 의도한 흐름을 문제없이 수행할 수 있는지다.

기본 원칙:

- 개발 서버가 있으면 브라우저에서 실제 화면을 연다.
- 클릭, 입력, 탭 전환, 모달 열기/닫기, 검색 같은 핵심 상호작용을 실제로 검증한다.
- 콘솔 에러와 실패한 네트워크 요청이 없는지 함께 확인한다.
- 변경한 기능의 시작 조건, 사용자 액션, 기대 결과를 최소 한 번은 실제 화면에서 끝까지 따라가 본다.

프론트 검증에서 우선하는 질문:

- 화면이 실제로 열리는가
- 사용자가 클릭/입력/이동한 결과가 의도대로 반영되는가
- 로딩, 빈 상태, 오류 상태가 깨지지 않는가
- 콘솔 에러나 실패한 네트워크 요청 없이 흐름이 끝나는가

생략 가능한 경우:

- 문서만 수정한 경우
- UI에 영향이 없는 순수 내부 리팩터링인 경우
- 로컬 실행이 불가능한 명확한 환경 제약이 있는 경우

생략했다면 이유를 남긴다.

## Completion Checklist

프론트 작업을 끝냈다고 보기 위한 최소 조건은 아래와 같다.

- 관련 codemap과 UI 설계 문서를 읽고 반영했다.
- 상태, fetch, 공용 컴포넌트 배치가 기존 기준과 충돌하지 않는다.
- 필요한 경우 타입체크와 빌드를 통과했다.
- UI 영향이 있는 변경이면 실제 화면에서 핵심 사용자 흐름을 검증했거나, 생략 이유를 남겼다.
- 프론트 작업의 완료 판단은 점수 문서가 아니라 실제 UI가 의도대로 작동하는지로 한다.
