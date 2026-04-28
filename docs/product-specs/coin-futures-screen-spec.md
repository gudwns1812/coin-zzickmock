# 코인 선물 MVP 화면 명세

## 목적

이 문서는 코인 선물 모의투자 플랫폼 MVP의 화면 구조와 각 화면이 요구하는 데이터, 상호작용, 상태 처리를 정의한다.
제품 기준이 "무엇을 만들지"를 정했다면, 이 문서는 "어떤 화면을 어떤 책임으로 나눌지"를 고정하는 데 목적이 있다.

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

사용자가 자신의 주요 자산 지표를 확인하고, 현재 시장의 주요 지표(가격, 거래대금, 펀딩비 등)와 다른 사용자들의 실현 수익률 랭킹을 한눈에 파악하게 만든다.

### 주요 섹션

1. **상단 요약 카드 (3종)**: 총 자산, 총 수익, 오늘 수익
2. **코인 시세 테이블**: 코인명(아이콘 포함), 가격, 24h 변동, Mark Price, Funding Rate, Index Price, 24h 거래대금
3. **실현 수익률 랭킹 리스트**: 순위, 닉네임, 지갑 자산, 실현 수익률

### 필요한 데이터

- 사용자 계정 요약 (총 자산, 총 수익, 오늘 수익)
- 마켓 데이터 리스트 (심볼별 가격, 변화율, Bitget `usdtVolume` 기반 24h 거래대금, Funding Rate, Mark Price, Index Price)
- 실현 수익률 랭킹 데이터 (상위 랭커 닉네임, `wallet_balance`, 초기 지급액 대비 수익률)
- 랭킹 데이터는 가입 직후와 지갑 잔고 변경 이벤트 이후 Redis ZSET의 해당 멤버를 즉시 갱신하고, 1시간 단위 전체 재집계로 보정한다.

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
- REST 시장 목록/상세와 SSE 실시간 시세 payload는 `turnover24hUsdt`를 숫자로 포함한다.
- 실현 수익률 랭킹은 미실현 손익이 아니라 실제 지갑 USDT에 반영된 정산 손익과 수수료 기준으로 표시된다.
- 신규 가입자는 기본 지갑 잔고 `100000 USDT`로 랭킹 인덱스에 추가되며, 이후 포지션 종료/체결 수수료처럼 지갑 잔고가 바뀌는 이벤트가 발생할 때 해당 멤버의 ZSET 점수만 갱신된다.

## 화면 4. 심볼 상세 `/markets/[symbol]`

### 목표

사용자가 특정 심볼의 시장 상태를 확인하고, 같은 화면에서 주문과 포지션 관리를 할 수 있게 한다.

### 상단 헤더

- `BTCUSDT`, `ETHUSDT` 심볼 selector. 현재 심볼은 선택 상태로 표시하고, 다른 심볼을 누르면 `/markets/{symbol}`로 이동한다.
- 심볼명
- 현재가
- 24h 변화율
- mark price
- index price
- funding rate
- 다음 funding 시각 또는 남은 시간

다음 funding 정보는 백엔드 market summary의 `nextFundingAt`에서 온다. 프론트엔드는 해당 timestamp를 기준으로 표시용 카운트다운만 갱신하며, KST 01:00/09:00/17:00 경계 규칙을 화면 코드에 하드코딩하지 않는다.

### 중앙 차트 영역

- 캔들 차트
- 기간 전환 (`1m`, `3m`, `5m`, `15m`, `1h`, `4h`, `12h`, `1D`, `1W`, `1M`)
- 사용자가 선택한 차트 기간은 브라우저에 저장되어 심볼 이동이나 화면 재진입 후에도 이어서 사용한다.
- 거래량 보조 표시
- 차트를 왼쪽으로 이동하면 더 오래된 캔들을 서버에서 이어서 로드
- 최초 진입 시에만 로드된 캔들의 가장 빠른 timestamp부터 가장 늦은 timestamp까지를 `setVisibleRange`로 한 번 정하고, 이후 live/refetch/사용자 이동/"최신 보기"는 차트를 계속 고정하지 않는다. 오른쪽 여백은 차트 `rightOffset` 설정으로 유지한다.
- 차트 헤더에 심볼과 커서가 가리키는 캔들의 `open/high/low/close` 표시
- 보조지표가 켜져 있으면 차트 헤더에서 커서가 가리키는 캔들의 보조지표 가격을 접었다 펼칠 수 있어야 한다
- 차트가 자동으로 최신 시각으로 복귀하지 않고, 헤더 우측 "최신 보기" 버튼으로만 최신 위치 복귀
- 차트 내부 indicator row에서 `EMA`, `SMA`, `Bollinger Bands`를 켜고 끌 수 있고, 기본값은 모두 꺼진 상태
- 보조지표 선은 차트 본문을 가리는 긴 이름 라벨 대신 가격축의 현재 값을 우선 보여준다

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
- 가격 입력은 화면/모달이 열린 순간 또는 사용자가 order book 가격을 선택한 순간의 snapshot 값을 한 번만 채운다. 이후 최신가, mark price, order book 업데이트가 들어와도 사용자가 입력 중인 가격을 자동으로 덮어쓰지 않는다. 단, TP/SL 신규 편집 값은 사용자가 직접 입력하도록 빈 값으로 시작하며 기존 TP/SL 주문이 있을 때만 기존 trigger price를 채운다.
- Account 영역은 주문 미리보기 수수료가 아니라 계정 상태를 보여준다:
  - `USDT balance`: wallet balance + total unrealized PnL
  - `Wallet balance`: 미실현 손익을 제외한 지갑 잔고
  - `Available`: 사용 가능 잔고
  - `Unrealized PnL`: 전체 열린 포지션의 미실현 손익 합계
  - `ROI`: total unrealized PnL / total open initial margin

### 하단 탭

- 포지션: 전체 심볼의 열린 포지션을 보여준다
- 포지션 히스토리: 완전히 종료된 포지션만 보여준다
- Open orders: 전체 심볼의 미체결 주문을 보여준다
- Order history: 전체 심볼의 주문 생성/상태 히스토리를 보여준다. 체결 전 pending limit 주문도 생성 즉시 포함된다.

### 핵심 상호작용

- 주문 입력 변경 시 예상 증거금과 청산가 즉시 재계산
- 사용자가 차트를 과거 구간으로 이동해도 자동 최신 포커스 복귀 없이 현재 보던 위치가 유지된다
- 사용자는 필요할 때만 "최신 보기" 버튼으로 가장 최근 캔들 영역으로 이동한다
- indicator는 fresh candle history가 있을 때만 렌더링되고, stale/missing history에서는 config만 유지한 채 표시를 숨긴다
- 지정가 주문은 현재 체결 가능 여부에 따라 `Maker` 또는 `Taker` 예상 배지 표시
- 현재 포지션이 있으면 부분 종료 또는 전체 종료 액션 제공
- 선택된 심볼과 같은 포지션의 `Mark Price`, `Unrealized PnL`, `ROE`, 계정 영역의 전체 미실현 손익은 실시간 market snapshot의 mark price로 표시용 값을 재계산한다. LONG 미실현 손익은 `(markPrice - entryPrice) * quantity`, SHORT 미실현 손익은 `(entryPrice - markPrice) * quantity`, ROE는 `unrealizedPnl / margin`이다. margin이 0이거나 유한하지 않으면 `NaN`을 표시하지 않는다. 서버 조회값은 새로고침/체결 이벤트 이후의 authoritative snapshot이고, 프론트 재계산은 저장/정산에 쓰지 않는 display-only 값이다.
- 포지션 종료 버튼은 모달을 열고, 사용자는 `Market` 즉시 종료 또는 `Limit` 종료 주문을 선택한다
- `Limit` 종료 주문은 체결 전까지 Open orders에 남고 취소할 수 있다
- 열린 포지션 카드는 현재 보유 수량인 `Size = quantity + base asset`을 표시한다. `Size`는 아직 열린 수량이고, `Close amount`는 누적 종료 체결 수량이므로 서로 대체하지 않는다.
- 선택 심볼 포지션의 `Mark Price`, `Unrealized PnL`, `ROE`, 총 미실현 손익은 market SSE의 최신 `markPrice`로 표시 전용 재계산을 할 수 있다. 공식 식은 simulation rules의 `unrealizedPnl = (markPrice - entryPrice) * quantity`(LONG), `(entryPrice - markPrice) * quantity`(SHORT), `roi = unrealizedPnl / margin`이며, `margin`이 0 또는 non-finite이면 화면 ROE는 `0`으로 처리한다.
- 포지션 응답은 `accumulatedClosedQuantity`, `pendingCloseQuantity`, `closeableQuantity`를 제공한다. 화면의 `Close amount`는 누적 종료 체결 수량인 `accumulatedClosedQuantity`만 의미한다.
- `pendingCloseQuantity`는 같은 심볼/방향/마진 모드의 미체결 close 주문 effective exposure이고, `closeableQuantity = max(0, quantity - pendingCloseQuantity)`이다. OCO group이 있는 TP/SL sibling은 group별 `max(quantity)`로 한 번만 계산한다. 두 값은 예약 상태/호환 필드이며 `Close amount` 라벨로 표시하지 않는다.
- close 입력의 최대값과 클라이언트 검증은 `closeableQuantity`가 아니라 현재 보유 `quantity` 기준이다. pending close 수량이 보유 수량과 같아도 새 close 주문은 제출할 수 있고, 백엔드는 접수 후 pending close cap을 조정한다.
- market close, limit close 체결, liquidation, 새 close 주문 접수로 포지션 수량 또는 pending close effective exposure가 cap을 넘으면 같은 포지션의 pending close exposure가 남은 포지션 수량을 넘지 않도록 조정한다. manual close 주문은 TP/SL OCO bucket보다 우선 보존하고, 같은 OCO group의 TP/SL sibling quantity는 함께 맞춘다.
- 포지션은 order-backed `takeProfitPrice`, `stopLossPrice`를 표시할 수 있다. 화면은 `Position TP/SL` 값을 기본 표시만 하고, edit icon을 눌렀을 때 편집기를 연다. 사용자가 TP/SL을 수정하면 백엔드는 현재 mark price 기준으로 이미 발동된 가격을 거절하고, pending conditional `CLOSE_POSITION` 주문을 생성/교체/취소한다.
- TP/SL 조건부 주문은 Open orders와 Order history에 `TP Close`, `SL Close`로 표시하며 trigger price를 가격 기준으로 보여준다. 체결 이력의 execution price는 실제 체결가다.
- Open orders와 Order history는 주문 시간을 가장 왼쪽 컬럼에 둔다
- Open orders에서는 예상 증거금과 예상 수수료를 표시하지 않는다
- 포지션 히스토리는 심볼, Long/Short, 레버리지, Cross/Isolated, 오픈 시간, 평균 진입/탈출 가격, 포지션 규모, PnL, ROI, 종료 시간, 종료 사유를 표시한다
- 포지션 히스토리의 `PnL`/`realizedPnl`은 gross PnL이 아니라 오픈 수수료, 종료 수수료, funding cost를 반영한 최종 순손익이다. 백엔드는 `grossRealizedPnl`, `openFee`, `closeFee`, `totalFee`, `fundingCost`, `netRealizedPnl`을 함께 내려줄 수 있다.

### 빈 상태

- 포지션 없음
- 미체결 주문 없음
- 최근 체결 없음
- 종료된 포지션 히스토리 없음

### 오류 상태

- 주문 실패 이유를 필드 또는 배너로 설명
- 시세 stale 시 주문 버튼 비활성화 여부를 명확히 표시

### 수용 기준

- 상세 화면 하나에서 시장 확인과 주문까지 끝낼 수 있다
- 사용자는 제출 전에 비용과 위험을 읽을 수 있다
- 열린 포지션과 미체결 주문을 놓치지 않는다
- 포지션 히스토리는 완전 종료 시점에만 생성된다
- 포지션 히스토리 PnL은 trading fee와 funding cost를 반영한 net PnL이다.
- pending close 주문이 있는 상태에서 포지션을 종료해도 같은 포지션의 stale close 주문이 체결 가능한 상태로 남지 않는다.
- TP/SL 편집은 `Position TP/SL` 편집 아이콘을 통해서만 열리고, 현재 mark price 기준으로 즉시 발동될 값을 저장하지 않으며, 발동 시 `TAKE_PROFIT` 또는 `STOP_LOSS` 종료 사유와 함께 포지션 히스토리에 남는다. 편집기 가격 입력은 신규 값에 mark price 기본값을 넣지 않고 빈 값으로 시작한다. 기존 TP/SL이 있으면 해당 trigger price만 한 번 채우며 live update로 덮어쓰지 않는다.
- `Close amount`는 pending close나 closeable quantity가 아니라 누적 체결 종료 수량이다. 신규 포지션에서는 0이고, 부분 종료 체결 후에만 증가한다.
- pending close 주문이 이미 보유 수량 전체를 덮어도 추가 close 주문 제출 UI/API를 막지 않으며, 접수 후 cap reconciliation으로 pending close 총량을 정리한다.

## 화면 5. 마이페이지 `/mypage`

### 목표

사용자가 일반 계정 정보, 자산, 포인트 흐름을 계정 영역에서 분리해 확인하게 만든다.
`/portfolio`는 호환성만 유지하고 `/mypage`로 리다이렉트한다.

### 공통 구조

- 왼쪽 패널:
  `Info`, `Assets`, `Point`
- 본문:
  현재 하위 페이지의 상세 정보
- 인증:
  `/mypage`, `/mypage/assets`, `/mypage/points`는 로그인 필요

### `/mypage`

- 사용자 이름
- 이메일
- 휴대폰 번호
- 회원 ID
- 지갑 잔고
- 사용 가능 잔고
- 열린 포지션 수
- 포인트 잔액

### `/mypage/assets`

- 앱의 밝은 금융 대시보드 톤에 맞춘 assets 패널을 사용한다.
- 패널 오른쪽에는 `wallet_history` API의 최근 30일 wallet balance를 차트 라이브러리 기반 선 그래프로 표시한다.
- 거래소성 액션 버튼(`Deposit`, `Buy crypto`, `Withdraw`, `Transfer`, `Sell crypto`)은 제공하지 않는다.
- 총 평가 잔고는 wallet balance와 열린 포지션의 unrealized PnL을 합산해 표시한다.
- 사용 가능 잔고를 함께 표시한다.
- wallet history가 비어 있으면 백엔드의 현재 잔고 fallback point를 사용하고, 차트 라벨은 KST 날짜로 표시한다.
- 하단에는 KST 일자별 `netRealizedPnl` 캘린더를 제공한다.
- 일별 값은 포지션 히스토리의 `closedAt`을 `Asia/Seoul` 날짜로 변환한 뒤 `netRealizedPnl`을 합산한다.

### `/mypage/points`

- 현재 포인트 잔액
- 포인트 적립, 교환권 신청 차감, 교환권 환불 이력
- 상점 이동 링크

### 상태 처리

- 인증되지 않은 사용자는 middleware에서 `/login`으로 리다이렉트한다.
- `/mypage`의 기본 정보가 없으면 값 자리에 `-` 또는 0을 표시하고, 계정 영역 자체는 유지한다.
- `/mypage/assets`의 포지션 히스토리가 비어 있으면 캘린더를 0원 셀로 표시한다.
- `/mypage/points`의 포인트 이력이 비어 있으면 빈 이력 문구를 표시한다.
- API 장애 시 서버 렌더링 단계의 fallback 데이터는 화면 붕괴를 막는 용도이며, 실제 잔액/포인트 확정값은 다음 정상 API 응답으로 갱신한다.

### 수용 기준

- `/mypage`는 사용자 기본 정보를 보여준다.
- `/mypage/assets`는 거래소 액션 버튼 없이 assets 패널과 KST 일별 실현손익 캘린더를 보여준다.
- `/mypage/points`는 현재 포인트와 point history를 보여준다.
- `/portfolio`는 `/mypage`로 리다이렉트된다.

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
- 다음 funding timestamp
- 현재 포지션 보유 여부

## 화면 7. 상점 `/shop`

### 목표

사용자가 실현 손익으로 모은 포인트를 소비할 수 있게 만든다.

### MVP 역할

상점은 투자 기능의 핵심은 아니지만,
"수익을 보면 보상이 생긴다"는 루프를 시각적으로 완성하는 보조 시스템이다.

### 주요 섹션

1. 현재 포인트 잔액
2. DB에서 읽은 구매 가능한 아이템 목록
3. 커피 교환권 신청 모달
4. sold-out / 유저별 구매 제한 / 포인트 부족 상태

### MVP 아이템 방향

- 커피 교환권을 DB seed/migration 데이터로 제공한다.
- 상품은 DB 운영 데이터로 관리한다.
- 상품은 `active`, `total_stock`, `sold_quantity`, `per_member_purchase_limit`을 가진다.
- 별도 `sellable` 플래그는 두지 않고 판매 가능 여부는 `active`, 재고, 구매 제한, 포인트 잔액으로 계산한다.
- `total_stock = null`은 무제한 재고이며, 유한 재고의 잔여 수량은 `max(total_stock - sold_quantity, 0)`으로 계산한다.
- `per_member_purchase_limit = null`은 유저별 제한 없음이며, 제한이 있으면 `max(per_member_purchase_limit - purchase_count, 0)`으로 계산한다.
- 상태 우선순위는 비활성, 품절, 유저별 구매 제한, 포인트 부족 순서로 한 가지 대표 사유를 보여준다.

실제 투자 성능에 직접 영향을 주는 pay-to-win 아이템은 두지 않는다.

### 핵심 상호작용

- 유저가 커피 교환권 구매를 누른다.
- 모달에서 휴대폰 번호를 입력한다.
- 유효한 번호는 숫자와 하이픈만 허용하고 서버에서 10~11자리 숫자로 정규화한다.
- 신청 성공 시 포인트는 즉시 차감되고 `PENDING` 교환권 요청이 생성된다.
- 관리자에게 SMTP 알림을 보낸다. 수신자는 `coin.reward.notification.admin-email` 설정값이며 기본값은 `gudwns1812@naver.com`이다.
- SMTP 실패는 요청을 롤백하지 않고 request id와 수신자를 포함해 로그로 남긴다.
- MVP는 알림 시도 이력을 별도 테이블로 저장하거나 자동 재시도하지 않는다. 알림 실패 시에도 관리자 페이지의 `PENDING` 목록이 처리 기준 데이터다.
- Discord 등 추가 채널은 같은 notification boundary에 붙이는 후속 확장으로 둔다.

## 화면 8. 관리자 교환권 처리 `/admin/reward-redemptions`

### 목표

운영자가 커피 교환권 신청을 확인하고, 승인 완료 또는 반려를 처리한다.

### 권한

- 프론트 라우트는 로그인 필요.
- 실제 관리자 권한은 백엔드가 persisted `ADMIN` role로 강제한다.

### 주요 섹션

- 상태 탭:
  `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`
- 요청자 member id
- 상품명과 포인트
- 제출한 휴대폰 번호
- 신청 시각
- 관리자 메모

### 핵심 상호작용

- `PENDING -> APPROVED`: 승인 완료 처리
- `PENDING -> REJECTED`: 관리자 반려, 포인트 환불, 재고/유저 구매 카운트 복구
- `PENDING -> CANCELLED`: 사용자 취소, 포인트 환불, 재고/유저 구매 카운트 복구
- `APPROVED`는 취소/환불할 수 없다.
- `APPROVED`, `REJECTED`, `CANCELLED`는 terminal 상태이며 추가 상태 전이가 없다.
- 중복 클릭이나 이미 terminal 상태인 요청의 재처리는 백엔드에서 거절하고 포인트/재고/구매 카운트를 다시 변경하지 않는다.
- 관리자 처리 행위는 요청 행에 `adminMemberId`, `adminMemo`, `sentAt` 또는 `cancelledAt`으로 남긴다. `sentAt`은 DB/API 하위 호환을 위해 유지하는 필드명이며 새 계약에서는 승인 시각을 뜻한다. 별도 감사 로그 테이블은 MVP 범위 밖이다.
- 기존 `SENT`/`CANCELLED_REFUNDED` 저장값은 마이그레이션으로 `APPROVED`/`REJECTED`에 매핑하고, 기존 관리자 필터/엔드포인트는 후속 UI 전환이 끝날 때까지 호환 alias로 유지한다.

### 환불 정확성

- 관리자/사용자 전이는 `status = PENDING` 조건을 포함한 DB conditional update로 한 요청만 terminal 상태를 claim한 뒤 수행한다.
- affected rows가 `0`이면 요청을 다시 조회해 없는 요청은 `404`, 다른 사용자의 사용자 취소는 `403`, 이미 terminal 상태이거나 stale 전이는 `409`로 응답한다.
- 취소/환불은 같은 트랜잭션 안에서 요청 상태 변경, 포인트 환불, 환불 이력 생성, `sold_quantity`와 `purchase_count` 복구를 처리한다.
- 복구 연산은 현재 값이 0이면 더 줄이지 않는 guarded decrement를 사용해 음수 재고와 음수 구매 카운트를 방지한다.
- 트랜잭션 실패 시 요청 상태와 포인트/재고/구매 카운트 변경은 함께 롤백된다.

### 수용 기준

- 포인트를 어디서 벌고 어디에 쓰는지 사용자가 이해할 수 있다
- 신청 결과가 즉시 계정과 관리자 페이지에 반영된다
- 환불은 정확히 한 번만 발생한다

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
- `MyPageShell`
- `DailyPnlCalendar`
- `RiskWarningBanner`

## 현재 프론트 구조에서의 매핑 전략

### 재사용 우선

- [frontend/app/(main)/layout.tsx](</Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/layout.tsx>)
- [frontend/components/ui/Sidebar.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/ui/Sidebar.tsx)
- [frontend/components/ui/shared/header/Header.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/ui/shared/header/Header.tsx)

### 교체 우선

- [frontend/app/(main)/stock/page.tsx](</Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock/page.tsx>)
- [frontend/app/(main)/stock/[code]/page.tsx](</Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock/[code]/page.tsx>)
- [frontend/app/(main)/portfolio/page.tsx](</Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/portfolio/page.tsx>)
- [frontend/api/stocks.ts](/Users/hj.park/projects/coin-zzickmock/frontend/api/stocks.ts)
- [frontend/hooks/useRealTimeStock.ts](/Users/hj.park/projects/coin-zzickmock/frontend/hooks/useRealTimeStock.ts)

## 유지보수 기준

- 주문/포지션/미체결 주문의 계산 규칙은 [coin-futures-simulation-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-simulation-rules.md)에 둔다.
- 화면 구조가 바뀌면 이 문서에서 route, 필수 섹션, 수용 기준을 함께 갱신한다.
- 실제 구현 순서와 완료 기록은 `docs/exec-plans/`에서 관리한다.
