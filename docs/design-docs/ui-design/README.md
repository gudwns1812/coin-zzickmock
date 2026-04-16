# UI Design Docs

이 폴더는 `coin-zzickmock` 프론트엔드의 UI 디자인 기준을 주제별로 나눈 문서 묶음이다.
단일 `DESIGN.md` 대신, 실제 구현 시 자주 참조하는 기준별로 분리해 관리한다.

이 문서 묶음은 다음 상황에서 먼저 본다.

- 새 화면을 설계할 때
- 카드, 차트, 리스트, 모달 같은 공통 패턴을 만들 때
- 기존 UI를 리디자인하거나 정리할 때
- shared 컴포넌트 승격 여부를 판단할 때

## 문서 구조

- [01-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/01-foundations.md)
  시각 언어, 브랜드 톤, 토큰, 타이포그래피, 색상 의미 체계
- [02-layouts-and-surfaces.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/02-layouts-and-surfaces.md)
  앱 프레임, 페이지 그리드, 데스크톱 정책, 배경과 카드 표면 규칙
- [03-data-display.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/03-data-display.md)
  카드, 차트, 숫자, 리스트, 종목 행 디자인 규칙
- [04-inputs-and-overlays.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/04-inputs-and-overlays.md)
  버튼, 입력, 검색, 모달, 오버레이, 액션 영역 규칙
- [05-motion-states-accessibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md)
  모션, 로딩/에러/빈 상태, 접근성, 상호작용 피드백
- [06-component-boundaries.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/06-component-boundaries.md)
  어떤 UI를 어디에 둘지, shared 승격 기준, 레거시 구조 해석

## 읽는 순서

새 화면을 만들 때는 아래 순서를 권장한다.

1. `01-foundations`
2. `02-layouts-and-surfaces`
3. `03-data-display`
4. 필요 시 `04`, `05`, `06`

## 운영 규칙

- 새 공통 시각 패턴이 생기면 관련 문서를 먼저 갱신한다.
- 같은 규칙이 여러 문서에 중복되면 더 상위 개념 문서로 올린다.
- 컴포넌트 하나만의 특수 규칙보다, 여러 화면에 재사용되는 기준을 우선 문서화한다.
- 새 디자인 제안은 이 폴더 문서 중 어떤 규칙을 따르고 어떤 규칙을 의도적으로 벗어나는지 설명할 수 있어야 한다.
