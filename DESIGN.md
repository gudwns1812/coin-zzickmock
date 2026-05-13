# Design

## Source of truth
- Status: Draft
- Last refreshed: 2026-05-13
- Primary product surfaces: 데스크톱 중심 코인 선물 모의투자 대시보드, markets 시세/리더보드, 포트폴리오, 마이페이지, 관심목록, 상점, 관리자 화면
- Evidence reviewed:
  - `/Users/hj.park/projects/coin-zzickmock/FRONTEND.md`
  - `/Users/hj.park/projects/coin-zzickmock/frontend/README.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/README.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/01-foundations.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/02-layouts-and-surfaces.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/03-data-display.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/04-inputs-and-overlays.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md`
  - `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/06-component-boundaries.md`
  - `/Users/hj.park/projects/coin-zzickmock/frontend/components/router/(main)/markets/MarketsLanding.tsx`

## Brand
- Personality: 안정적인 금융 워크스테이션, 정보 우선, 밝고 신뢰감 있는 블루 기반 대시보드
- Trust signals: 일관된 숫자 포맷, 손익 색상 의미, 얕은 카드 계층, 예측 가능한 CTA와 상태 메시지
- Avoid: 데이터보다 장식이 먼저 보이는 hero-first UI, 과한 네온/글래스모피즘, 화면마다 달라지는 색 의미, 장식 수식어가 많은 랭킹 copy

## Product goals
- Goals: 사용자가 시장 현황, 포트폴리오, 랭킹, 리워드/상점 흐름을 빠르게 파악하고 안전하게 모의 거래를 학습한다.
- Non-goals: 모바일 우선 소비형 피드, 실제 투자 조언, 과도한 게임화 문구 중심 경험
- Success signals: 핵심 수치의 빠른 스캔, 검색/선택/모달/팝오버 흐름의 끊김 없음, 빈/로딩/오류 상태의 명확한 복구 안내

## Personas and jobs
- Primary personas: 코인 선물 거래를 연습하는 데스크톱 사용자, 수익률/리더보드를 확인하는 경쟁형 사용자, 리워드/상점 운영 관리자
- User jobs: 시장 가격 확인, 주문/포지션 관리, 랭커와 내 순위 비교, 공개 포지션 엿보기, 리워드 교환 관리
- Key contexts of use: 데스크톱 브라우저, 데이터가 자주 변하는 대시보드, 인증된 사용자 중심 화면

## Information architecture
- Primary navigation: 헤더와 우측 보조 사이드바를 사용하는 로그인 후 메인 앱 프레임
- Core routes/screens: `/markets`, `/markets/[symbol]`, `/mypage`, `/watchlist`, `/shop`, `/admin`, 로그인/회원가입, 데스크톱 안내
- Content hierarchy: 요약 지표 → 시장/차트/목록 → 랭킹/포지션/액션 보조 패널 순으로 정보 밀도를 높인다.

## Design principles
- Principle 1: 숫자와 상태가 장식보다 먼저 읽혀야 한다.
- Principle 2: 같은 의미의 색, 간격, 카드 계층은 화면이 달라도 흔들리지 않아야 한다.
- Tradeoffs: 데스크톱 완성도와 정보 밀도를 모바일 단순 적층보다 우선한다. 시각 강조는 허용하되 데이터 가독성을 가리면 줄인다.

## Visual language
- Color: `main-blue`를 브랜드/기본 CTA로, 손익 양수는 녹색, 손실/오류/파괴는 빨강, 중립은 회색으로 유지한다.
- Typography: Pretendard Variable과 기존 `text-*-custom` 스케일을 우선한다.
- Spacing/layout rhythm: `gap-main`, `p-main`, `px-main-2` 등 기존 토큰을 사용한다.
- Shape/radius/elevation: `rounded-main`, 얕은 보더와 그림자 중심의 흰 카드 표면을 유지한다.
- Motion: hover, 선택, 오버레이 진입 같은 목적 있는 약한 모션만 사용한다.
- Imagery/iconography: 순위 1~4위는 번들 이미지를 작게 쓰고, 5위 이상은 숫자 배지로 표현한다.

## Components
- Existing components to reuse: markets route section, shared Input/Modal/Button 계열, 기존 카드/리스트/랭킹 배지 패턴
- New/changed components: markets 리더보드 검색은 별도 결과 칩을 만들지 않고 리더보드 row 영역을 검색 결과 row로 교체한다. 랭커 포지션 popover는 카드 전체를 아래로 밀지 않고, 말풍선 꼬리 위치가 선택 row 높이에 anchor된다.
- Variants and states: 랭킹 row 기본/hover/selected/compact, 검색 loading/empty/error, popover unauthenticated/locked/consuming/unlocked/empty snapshot
- Token/component ownership: route 전용 조합은 `frontend/components/router/(main)/markets/`에 두고, 두 화면 이상 반복될 때 shared 승격을 검토한다.

## Accessibility
- Target standard: 현재 데스크톱 우선 UI에서도 키보드 접근과 명확한 상태 표현을 유지한다.
- Keyboard/focus behavior: 검색 입력, row 선택, 닫기 버튼, CTA는 focus-visible 경로가 있어야 한다.
- Contrast/readability: 흰 카드 위 진한 텍스트와 손익 색상 대비를 유지한다.
- Screen-reader semantics: 아이콘 버튼은 `aria-label`, 이미지 순위 배지는 대체 텍스트를 제공한다.
- Reduced motion and sensory considerations: 반복/장식 모션은 최소화하고 상태 전달 모션보다 약하게 둔다.

## Responsive behavior
- Supported breakpoints/devices: 핵심 업무 화면은 데스크톱 최소 폭 기준으로 설계한다.
- Layout adaptations: 복잡한 분석 그리드는 임시 모바일 적층보다 기능 우선순위를 다시 정하는 방향으로 다룬다.
- Touch/hover differences: 현재 주 상호작용은 데스크톱 hover/focus를 기준으로 하되 클릭 영역은 충분히 크게 유지한다.

## Interaction states
- Loading: 카드나 row 영역 단위로 짧은 상태 문구 또는 스켈레톤을 제공한다.
- Empty: 데이터 집계 중, 검색 결과 없음, 열린 포지션 없음처럼 원인을 드러낸다.
- Error: 위협적이지 않은 빨강 톤으로 오류와 재시도 맥락을 표시한다.
- Success: 선택/완료/공개 포지션 확인 상태를 카드 내부에서 이어서 보여준다.
- Disabled: CTA 비활성 이유가 수량/권한/처리 중 상태와 함께 읽혀야 한다.
- Offline/slow network, if applicable: 카드 단위 실패 복구와 기존 데이터 유지가 우선이다.

## Content voice
- Tone: 간결하고 행위 중심, 내부 구현 모델보다 사용자가 하는 일을 설명한다.
- Terminology: “공개 포지션”, “확인 시각”, “엿보기권”, “실현 수익률”처럼 제품 문맥 용어를 사용한다.
- Microcopy rules: “저장된 스냅샷” 같은 내부 저장 모델 용어와 “왕좌/최상위 수익률/포디움” 같은 장식 수식어는 리더보드/팝오버 copy에서 피한다. 비로그인 엿보기 상태는 처리 중처럼 보이지 않게 로그인 필요와 로그인 CTA를 즉시 보여준다.

## Implementation constraints
- Framework/styling system: Next.js App Router, React 19, TypeScript, Tailwind CSS 4, React Query, Zustand
- Design-token constraints: `frontend/app/globals.css` 토큰과 UI design docs의 색/간격/반경 규칙을 우선한다.
- Performance constraints: 데이터 갱신 화면에서 불필요한 전역 상태 복제와 과도한 렌더링을 피한다.
- Compatibility constraints: 기존 `frontend/` 루트 구조와 `components/router` 레거시 배치를 존중한다.
- Test/screenshot expectations: UI 상호작용 변경은 타입체크/빌드와 가능하면 실제 브라우저 흐름 검증을 함께 수행한다.

## Open questions
- [ ] 리더보드 검색 결과가 여러 명일 때 최대 표시 개수와 정렬 기준 / product / 검색 UX의 예측 가능성
- [ ] 모바일에서 랭킹 popover를 drawer로 전환할지 여부 / design / 데스크톱 외 접근성
