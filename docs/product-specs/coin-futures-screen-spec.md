# 코인 선물 MVP 화면 명세

## 목적

이 문서는 코인 선물 모의투자 플랫폼 MVP의 화면 구조와 각 화면이 요구하는 데이터, 상호작용, 상태 처리를 정의한다.
제품 설계 초안이 "무엇을 만들지"를 정했다면, 이 문서는 "어떤 화면을 어떤 책임으로 나눌지"를 고정하는 데 목적이 있다.

이 문서는 아래 기준을 따른다.

- 지원 거래소: `Bitget`
- 지원 심볼: `BTCUSDT`, `ETHUSDT`
- 초기 잔고: `100000 USDT`
- 최대 레버리지: `50x`
- 마진 모드: `ISOLATED`, `CROSS`
- 포인트와 상점 포함

## 공통 UI 원칙

### 데스크톱 우선

현재 프론트가 데스크톱 금융 대시보드 구조를 이미 가지고 있으므로,
MVP는 최소 가로 폭을 유지한 데스크톱 우선 경험으로 간다.

### 공통 레이아웃

- 좌측 또는 우측 고정 사이드 영역
- 상단 헤더
- 중앙 콘텐츠 영역
- 핵심 숫자는 카드와 표로 빠르게 읽혀야 한다
- 주문, 포지션, 리스크처럼 중요한 정보는 스크롤 아래로 숨기지 않는다

### 공통 상태

모든 주요 화면은 아래 상태를 명시적으로 보여줘야 한다.

- loading
- empty
- error
- stale data
- unauthenticated

### 공통 배지와 색상 의미

- `LONG`: 상승 의미 색상
- `SHORT`: 하락 의미 색상
- `ISOLATED`: 개별 포지션 보호 강조
- `CROSS`: 계정 전체 증거금 공유 경고 강조
- `Maker`: 낮은 수수료 배지
- `Taker`: 높은 수수료 배지

## 라우트 맵

### 인증

- `/signup`
- `/login`

### 메인

- `/markets`
- `/markets/[symbol]`
- `/portfolio`
- `/watchlist`
- `/shop`

### 보조

- `/learn`
- `/only-desktop`

## 화면 1. 회원가입 `/signup`

### 목표

사용자가 모의투자 계정을 생성하고 최초 자산 `100000 USDT`를 지급받아 서비스에 진입하게 만든다.

### 필수 요소

- 이메일
- 비밀번호
- 비밀번호 확인
- 닉네임
- 약관/리스크 동의 체크
- "모의투자 서비스" 안내 문구

### 성공 동작

- 가입 완료 후 로그인 상태로 전환하거나 `/login`으로 보낸다
- 가입 성공 시 초기 계정, 포인트 지갑, 기본 관심 심볼을 생성한다

### 실패 상태

- 중복 이메일
- 약한 비밀번호
- 닉네임 중복
- 서버 오류

### 수용 기준

- 정상 입력 시 계정이 생성된다
- 생성 직후 `100000 USDT`가 준비된다
- 실패 시 어떤 필드가 문제인지 문장으로 보여준다

## 화면 2. 로그인 `/login`

### 목표

기존 사용자가 자신의 모의투자 계정으로 진입하게 만든다.

### 필수 요소

- 이메일
- 비밀번호
- 로그인 버튼
- 회원가입 이동 버튼

### 성공 동작

- 로그인 후 `/markets`로 이동한다
- 헤더와 사이드바에 사용자 계정 정보가 표시된다

### 실패 상태

- 잘못된 비밀번호
- 없는 이메일
- 잠긴 계정 또는 비활성 계정

## 화면 3. 마켓 대시보드 `/markets`

### 목표

사용자가 자신의 주요 자산 지표를 확인하고, 현재 시장의 주요 지표(가격, 거래량, 펀딩비 등)와 다른 사용자들의 수익률 랭킹을 한눈에 파악하게 만든다.

### 주요 섹션

1. **상단 요약 카드 (3종)**: 총 자산, 총 수익, 오늘 수익
2. **코인 시세 테이블**: 코인명(아이콘 포함), 가격, 24h 변동, 24h 거래량, Funding Rate, 시가총액, 점유율
3. **수익률 랭킹 리스트**: 순위, 닉네임, 총 자산, 수익률

### 필요한 데이터

- 사용자 계정 요약 (총 자산, 총 수익, 오늘 수익)
- 마켓 데이터 리스트 (심볼별 가격, 변화율, 거래량, Funding Rate, Market Cap, Dominance)
- 수익률 랭킹 데이터 (상위 랭커 닉네임, 자산, 수익률)

### 핵심 상호작용

- 심볼 클릭 시 `/markets/[symbol]` 이동
- 가격 실시간 업데이트 시 해당 가격 컴포넌트의 사각형 영역(Boundary)에 일시적인 플래시 효과 적용
- 정렬 기준 변경 (가격, 변동률 등)

### 빈 상태

- 랭킹 데이터가 없을 경우 "데이터 집계 중" 메시지 표시

### 오류 상태

- 외부 시세 수집 실패 시 상단 경고 배너 및 데이터 필드에 '-' 표시

### 수용 기준

- 3가지 핵심 자산 지표가 최상단에 요약되어 노출된다.
- 코인 시세 테이블을 통해 주요 시장 지표를 한눈에 비교할 수 있다.
- 가격 변동 시 영역 플래시 효과가 시각적으로 나타난다.
- 수익률 랭킹을 통해 서비스 내 활동성을 확인할 수 있다.

## 화면 4. 심볼 상세 `/markets/[symbol]`

### 목표

사용자가 특정 심볼의 시장 상태를 확인하고, 같은 화면에서 주문과 포지션 관리를 할 수 있게 한다.

### 상단 헤더

- 심볼명
- 현재가
- 24h 변화율
- mark price
- index price
- funding rate
- 다음 funding 시각

### 중앙 차트 영역

- 캔들 차트
- 기간 전환 (`1m`, `5m`, `15m`, `1h`, `4h`, `1d` 중 MVP 선택)
- 거래량 보조 표시

### 우측 주문 패널

- `LONG` / `SHORT` 전환
- `MARKET` / `LIMIT` 전환
- 가격 입력
- 수량 입력
- 레버리지 슬라이더 또는 입력
- 마진 모드 선택 (`ISOLATED`, `CROSS`)
- 예상 수수료
- 예상 사용 증거금
- 예상 청산가
- 주문 버튼

### 하단 탭

- 내 포지션
- 미체결 주문
- 최근 체결

### 핵심 상호작용

- 주문 입력 변경 시 예상 증거금과 청산가 즉시 재계산
- 지정가 주문은 현재 체결 가능 여부에 따라 `Maker` 또는 `Taker` 예상 배지 표시
- 현재 포지션이 있으면 부분 종료 또는 전체 종료 액션 제공

### 빈 상태

- 포지션 없음
- 미체결 주문 없음
- 최근 체결 없음

### 오류 상태

- 주문 실패 이유를 필드 또는 배너로 설명
- 시세 stale 시 주문 버튼 비활성화 여부를 명확히 표시

### 수용 기준

- 상세 화면 하나에서 시장 확인과 주문까지 끝낼 수 있다
- 사용자는 제출 전에 비용과 위험을 읽을 수 있다
- 열린 포지션과 미체결 주문을 놓치지 않는다

## 화면 5. 포트폴리오 `/portfolio`

### 목표

사용자가 자신의 전체 선물 계정 상태를 한눈에 보고, 리스크와 손익을 관리하게 만든다.

### 상단 요약 카드

- 총 자산
- 사용 가능 증거금
- 사용 중 증거금
- 미실현 손익
- 실현 손익
- 포인트 잔액

### 주요 섹션

1. 열린 포지션 테이블
2. 미체결 주문 테이블
3. 체결/손익 히스토리
4. 손익 차트
5. 리스크 경고 카드

### 포지션 테이블 컬럼

- 심볼
- 방향
- 마진 모드
- 레버리지
- 진입가
- mark price
- 청산가
- 수량
- 미실현 손익
- 액션

### 핵심 상호작용

- 포지션별 빠른 종료
- 히스토리 필터링
- 심볼별 보기
- 기간별 손익 보기

### 수용 기준

- 계정 전체 상태와 포지션 리스크를 동시에 읽을 수 있다
- 사용자는 손익과 포인트 변화를 함께 확인할 수 있다

## 화면 6. 관심 심볼 `/watchlist`

### 목표

사용자가 관심 심볼만 따로 모아서 빠르게 진입하게 만든다.

### MVP 단순화 원칙

- 그룹 기능은 두지 않는다
- `BTCUSDT`, `ETHUSDT` 중 체크해서 저장하는 단순 구조로 시작한다

### 필요한 데이터

- 사용자 watchlist
- 각 심볼의 최신 ticker
- funding rate
- 현재 포지션 보유 여부

## 화면 7. 상점 `/shop`

### 목표

사용자가 실현 손익으로 모은 포인트를 소비할 수 있게 만든다.

### MVP 역할

상점은 투자 기능의 핵심은 아니지만,
"수익을 보면 보상이 생긴다"는 루프를 시각적으로 완성하는 보조 시스템이다.

### 주요 섹션

1. 현재 포인트 잔액
2. 포인트 획득 규칙 카드
3. 구매 가능한 아이템 목록
4. 구매 이력

### MVP 아이템 방향

- 프로필 배지
- 닉네임 테두리
- 대시보드 테마
- 코멘트형 칭호

실제 투자 성능에 직접 영향을 주는 pay-to-win 아이템은 두지 않는다.

### 핵심 상호작용

- 아이템 구매
- 구매 성공 토스트
- 이미 보유한 아이템 표시

### 수용 기준

- 포인트를 어디서 벌고 어디에 쓰는지 사용자가 이해할 수 있다
- 구매 결과가 즉시 계정에 반영된다

## 화면 8. 학습 보조 `/learn`

### 목표

입문형 사용자가 선물 용어를 몰라도 서비스 사용을 포기하지 않게 만든다.

### MVP 구성

- `LONG`, `SHORT`
- `ISOLATED`, `CROSS`
- `Mark Price`
- `Funding Rate`
- `Liquidation`
- `Maker`, `Taker`

전용 페이지 또는 주문 패널의 도움말 모달로 시작할 수 있다.

## 공통 컴포넌트 후보

아래 컴포넌트는 여러 화면에서 반복 사용될 가능성이 높다.

- `AccountSummaryCard`
- `TickerStatCard`
- `FundingRateBadge`
- `MarginModeBadge`
- `OrderEntryPanel`
- `PositionTable`
- `OrderTable`
- `RewardPointCard`
- `ShopItemCard`
- `RiskWarningBanner`

## 현재 프론트 구조에서의 매핑 전략

### 재사용 우선

- [frontend/app/(main)/layout.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/layout.tsx)
- [frontend/components/ui/Sidebar.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/ui/Sidebar.tsx)
- [frontend/components/ui/shared/header/Header.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/ui/shared/header/Header.tsx)

### 교체 우선

- [frontend/app/(main)/stock/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock/page.tsx)
- [frontend/app/(main)/stock/[code]/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock/[code]/page.tsx)
- [frontend/app/(main)/portfolio/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/portfolio/page.tsx)
- [frontend/api/stocks.ts](/Users/hj.park/projects/coin-zzickmock/frontend/api/stocks.ts)
- [frontend/hooks/useRealTimeStock.ts](/Users/hj.park/projects/coin-zzickmock/frontend/hooks/useRealTimeStock.ts)

## 다음 상세화 대상

이 문서 다음에는 아래 순서로 더 쪼개면 된다.

1. 주문 패널 인터랙션 명세
2. 포지션/미체결 주문 테이블 컬럼 상세
3. 회원가입/로그인 API 명세
4. shop 아이템과 포인트 UX 상세
