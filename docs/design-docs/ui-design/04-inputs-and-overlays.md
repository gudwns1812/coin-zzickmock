# Inputs And Overlays

## 목적

이 문서는 버튼, 입력, 검색, 모달, 오버레이 같은 상호작용 UI를 정의한다.

## 버튼

기본 버튼 의미 체계:

- 기본 CTA: 블루 배경
- 파괴적 액션: 레드 배경
- 보조 액션: ghost 또는 약한 tinted 배경

규칙:

- 같은 의미의 버튼을 화면마다 새로 스타일링하지 않는다.
- 아이콘 버튼은 hit area를 충분히 확보한다.
- hover뿐 아니라 focus-visible도 있어야 한다.

## 입력

입력은 `components/ui/shared/Input.tsx` 확장을 우선 검토한다.

규칙:

- 배경은 투명 또는 흰색
- 보더는 옅은 회색
- focus 시 블루 계열 강조
- 보조 제어는 입력 우측 내부에 둔다
- 새 입력 스타일을 페이지 안에서 직접 다시 만들지 않는다

## 검색

검색은 이 제품의 핵심 탐색 도구다.

규칙:

- 검색 입력은 아이콘과 함께 하나의 덩어리로 설계한다.
- 결과 리스트는 종목 행 패턴을 그대로 따른다.
- 키보드 단축키, 모달 진입, 결과 클릭 흐름이 끊기지 않아야 한다.
- 검색 결과의 hover/active 상태는 명확해야 한다.
- markets 랭킹 검색은 ranked member만 대상으로 하고 결과 row에는 닉네임/rank/실현 수익률 맥락만 표시한다. raw id는 보이지 않으며 선택은 opaque target token으로 처리한다.

## 랭커 포지션 엿보기 popover

- 랭킹 행 또는 검색 결과 선택으로 오른쪽 popover를 연다. 열림 후 focus는 popover 제목 또는 첫 CTA로 이동하고 닫을 때 원래 행/검색 결과로 복귀한다.
- locked 상태는 position 정보를 가리지 않고 아예 렌더링하지 않는다. 보유 `POSITION_PEEK` item 수량, `아이템 사용` CTA, item 0개일 때 `/shop` 이동 링크만 보여준다.
- consuming 상태는 CTA pending, 중복 클릭 방지, 실패 메시지를 같은 popover 안에서 처리한다. 실패 후 item count는 최신 값으로 갱신한다.
- unlocked 상태는 저장 snapshot의 `createdAt`, rank/nickname context, public position rows를 보여준다. 무료 `새로고침` 버튼은 두지 않고 `다시 사용`만 새 snapshot 생성 CTA로 제공한다.
- empty snapshot은 성공 상태로 보이며 "열린 포지션이 없습니다"처럼 명확히 설명한다.
- forbidden field: TP/SL, close/edit/order action, history, raw member id는 locked/unlocked 어느 상태에도 표시하지 않는다.

## 모달과 오버레이

현재 모달은 중앙 정렬, 흰 배경, 어두운 backdrop, 내부 스크롤을 기본으로 한다.

규칙:

- 중요한 플로우는 모달 내부에서도 단계가 명확해야 한다.
- 모달은 카드보다 강한 레이어지만 지나치게 무겁게 만들지 않는다.
- 닫기 버튼, 외부 클릭 닫기, ESC 닫기 여부를 명시한다.
- 하단 fade는 스크롤 가능성 신호로 사용할 수 있다.

용도 기준:

- 가입
- 투자성향 변경
- 상세 편집
- 현재 화면 문맥을 유지한 채 처리할 일

이런 경우에 모달을 우선 사용한다.

## 액션 체크리스트

- 버튼 의미가 색과 위치에서 바로 읽히는가
- 입력과 검색이 shared 패턴을 따르는가
- 모달이 현재 화면 문맥을 유지하는 데 적절한가
- ESC, 닫기 버튼, 포커스 이동이 고려되었는가
