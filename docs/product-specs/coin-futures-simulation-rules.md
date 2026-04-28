# 코인 선물 MVP 시뮬레이션 규칙

## 목적

이 문서는 코인 선물 모의투자 플랫폼 MVP가 어떤 계산 규칙으로 주문, 수수료, 손익, 마진, 포인트를 처리하는지 정의한다.
핵심 목표는 "설명 가능한 일관성"이다.

사용자는 이 규칙을 몰라도 서비스를 쓸 수 있어야 하지만,
개발자와 에이전트는 같은 규칙으로 계산해야 한다.

## MVP 고정값

### 거래소와 심볼

- 데이터 소스: `Bitget`
- 지원 심볼: `BTCUSDT`, `ETHUSDT`
- 계약 타입: `USDT` 기준 선형 계약으로 단순화

### 계정

- 초기 지급 잔고: `100000 USDT`
- 기본 포인트: `0`

### 레버리지와 마진

- 최소 레버리지: `1x`
- 최대 레버리지: `50x`
- 지원 마진 모드: `ISOLATED`, `CROSS`

### 수수료

- maker fee rate: `0.00015`
- taker fee rate: `0.0005`

### funding

- 정산 단위: `8시간`
- 정산 대상: 오픈 포지션

## 용어 정의

### 수량 `quantity`

`BTCUSDT`라면 BTC 수량, `ETHUSDT`라면 ETH 수량이다.
예를 들어 `0.1 BTC`를 long하면 수량은 `0.1`이다.

### 진입가 `entryPrice`

포지션이 열린 체결 가격이다.

### 현재가 `lastPrice`

거래소의 최신 체결가다.

### Mark Price

포지션 평가와 청산 계산에 쓰는 기준 가격이다.
UI에는 최신 체결가와 구분해서 표시한다.

Mark-to-market 평가는 조회/청산 평가 시점에 최신 market data로 계산한다.
평가만으로 열린 포지션을 저장하거나 `open_positions.version`을 증가시키지 않는다.
심볼 상세 화면의 선택 심볼 포지션은 market SSE의 최신 `markPrice`를 사용해 표시 전용
mark-to-market을 프론트엔드에서 중복 계산할 수 있다. 이 중복 계산은 저장/정산에 사용하지
않고, 공식 입력은 `positionSide`, `entryPrice`, 현재 열린 `quantity`, `margin`, market
`markPrice`다. `margin`이 0 또는 non-finite이면 ROE 표시값은 `0`으로 처리해 `NaN`을
노출하지 않는다.

### 명목가치 `notional`

다음 식으로 계산한다.

`notional = price * quantity`

## 주문 규칙

### 시장가 주문

- 제출 즉시 체결을 시도한다
- 체결 타입은 항상 `taker`다
- 체결 가격은 최신 체결가를 기본으로 한다
- 필요하면 후속 단계에서 작은 슬리피지를 추가할 수 있지만, MVP 1차 구현은 `latest trade price` 그대로 시작한다

### 지정가 주문

지정가 주문은 제출 직후 시장을 먹는 주문인지, 호가창에 남는 주문인지에 따라 maker/taker가 갈린다.

#### LONG limit

- 사용자가 `10000`에 long limit를 걸었고 최신 체결가가 `9999` 이하로 내려가면 체결 대상으로 본다
- 제출 시점에 이미 최신 체결가가 `10000` 이하라면 즉시 체결 가능한 주문이므로 `taker`
- 제출 이후 대기하다가 이전 가격에서 현재 가격으로 내려오는 이동 구간에 지정가가 포함되면 `maker`

#### SHORT limit

- 사용자가 `10000`에 short limit를 걸었고 최신 체결가가 `10000` 이상으로 올라가면 체결 대상으로 본다
- 제출 시점에 이미 최신 체결가가 `10000` 이상이라면 즉시 체결 가능한 주문이므로 `taker`
- 제출 이후 대기하다가 이전 가격에서 현재 가격으로 올라가는 이동 구간에 지정가가 포함되면 `maker`

#### Pending limit 체결 순서

시세 수집부는 처리부에 이전 가격, 현재 가격, 이동 방향(`UP`/`DOWN`/`UNCHANGED`)을 함께 전달한다.
첫 시세처럼 이전 가격이 없거나 가격이 변하지 않은 이벤트는 pending limit 체결을 시도하지 않는다.

- 가격 상승(`UP`) 이벤트는 매도 성격 주문인 `open short`, `close long`만 후보로 본다
- 가격 하락(`DOWN`) 이벤트는 매수 성격 주문인 `open long`, `close short`만 후보로 본다
- 상승 구간에서는 지정가 오름차순으로 체결한다
- 하락 구간에서는 지정가 내림차순으로 체결한다
- 같은 지정가에서는 먼저 생성된 주문을 먼저 체결한다
- pending maker 주문의 체결 가격과 수수료 기준 가격은 현재가가 아니라 주문의 지정가다
- 각 주문은 하나씩 claim/fill하며, 종료 주문은 체결 직전 열린 포지션 수량을 다시 확인한다

### 포지션 종료 주문

포지션 종료 주문은 기존 포지션을 줄이는 목적의 주문이며 주문 계약에는 `OPEN_POSITION` 또는
`CLOSE_POSITION` 목적을 기록한다.

- Market 종료: 최신 체결가로 즉시 종료하며 `taker` 수수료를 적용한다
- LONG limit 종료: 최신 체결가가 지정가 이상이 되면 체결 대상으로 본다
- SHORT limit 종료: 최신 체결가가 지정가 이하가 되면 체결 대상으로 본다
- Limit 종료 주문은 체결 전까지 미체결 주문으로 남고 취소할 수 있다
- 동일 계정/심볼/방향/마진 모드의 pending 종료 주문 수량 합계는 OCO-aware effective exposure 기준으로 최종 정합화 이후 현재 열린 포지션 수량을 초과할 수 없다
- 새 종료 주문은 기존 pending 종료 주문 합계가 이미 현재 열린 포지션 수량과 같거나 더 큰 상태에서도 접수할 수 있다. 접수 후 같은 트랜잭션 흐름에서 cap reconciliation을 수행해 전체 pending 종료 주문 수량을 현재 열린 포지션 수량 이하로 맞춘다.
- pending 종료 주문의 effective exposure 공식은 `sum(ungrouped pending CLOSE_POSITION quantity) + sum(max(quantity) per ocoGroupId)`이다. scope는 `memberId + symbol + positionSide + marginMode`이고 `FILLED`, `CANCELLED` 주문은 제외한다.
- 초과 시 새 주문, market close 이후 남은 주문, pending 종료 주문 체결 이후 남은 주문, liquidation 이후 남은 주문을 모두 포함해 실행 가능성이 낮은 종료 주문부터 줄이거나 취소한다. 단, manual close 주문은 TP/SL OCO bucket보다 우선 보존한다.
  - LONG 종료 주문은 현재가보다 더 높은 지정가가 덜 실행되기 쉬우므로 지정가 내림차순으로 줄인다. 예: 현재가 `105`, close long `112`, `108`이 있으면 `112`를 먼저 줄인다.
  - SHORT 종료 주문은 현재가보다 더 낮은 지정가가 덜 실행되기 쉬우므로 지정가 오름차순으로 줄인다. 예: 현재가 `95`, close short `88`, `92`가 있으면 `88`을 먼저 줄인다.
  - 같은 지정가에서는 기존 주문 우선권을 보존하기 위해 더 나중에 생성된 주문을 먼저 줄이고, 생성 시각까지 같으면 `orderId` 오름차순으로 결정한다.
- 일부만 줄어든 주문은 `PENDING` 상태로 수량을 조정하고, 전량 줄어든 주문은 `CANCELLED`로 남겨 주문 이력을 보존한다
- 이 cap은 포지션을 늘리는 `OPEN_POSITION` 주문에는 적용하지 않는다
- 새 종료 주문 요청은 기존 pending 종료 주문 합계가 열린 포지션 수량을 이미 덮고 있어도 보유 수량 이하이면 우선 접수한 뒤 같은 cap reconciliation 규칙으로 pending 주문 총량을 조정한다.
- 포지션 API는 누적 종료 체결 수량인 `accumulatedClosedQuantity`를 내려주며, 화면의 `Close amount` 라벨은 이 값만 의미한다.
- 포지션 API는 호환 필드로 같은 포지션의 pending 종료 주문 effective exposure인 `pendingCloseQuantity`와 `closeableQuantity = max(0, quantity - pendingCloseQuantity)`도 내려준다. 두 필드는 예약/입력 안내용이며 `Close amount`로 표시하지 않는다.

포지션 종료, pending 종료 주문 체결, 청산 종료는 열린 포지션의 `version`을 조건으로 하는 낙관적 잠금 mutation이다.
이미 다른 요청이 포지션을 변경했다면 계정 정산, 포지션 이력, 리워드 지급, 응답용 체결 이벤트 발행 전에 충돌로 중단한다.

### 포지션 TP/SL

TP/SL은 열린 포지션 컬럼이 아니라 pending conditional `CLOSE_POSITION` 주문으로 관리한다.
`open_positions.take_profit_price`, `open_positions.stop_loss_price`는 legacy 호환 컬럼이며 application/domain read/write/trigger path의 source of truth가 아니다.
TP/SL 편집 API는 최신 mark price를 읽고, 저장 직후 이미 발동되는 값을 거절한 뒤 같은 포지션 scope의 기존 pending TP/SL 조건부 주문을 취소하고 새 주문을 만든다.

- TP/SL 주문 계약: `orderPurpose = CLOSE_POSITION`, `status = PENDING`, `triggerPrice != null`, `triggerType in (TAKE_PROFIT, STOP_LOSS)`, `triggerSource = MARK_PRICE`, `limitPrice = null`, `orderType = MARKET`
- 같은 `memberId + symbol + positionSide + marginMode + triggerType`에는 active pending TP/SL 주문이 하나만 존재한다. DB는 active pending conditional close order에만 채워지는 `active_conditional_trigger_type` unique key로 이 중복을 방지한다.
- TP와 SL이 함께 저장되면 같은 `ocoGroupId`를 공유하고, 둘 중 하나가 체결되면 sibling은 `CANCELLED`가 된다
- V9의 legacy `open_positions.take_profit_price`, `open_positions.stop_loss_price` 값은 V11 migration에서 pending conditional close order로 backfill한다. 이후 legacy 컬럼은 읽기/트리거 source가 아니다.
- 수동 취소는 선택한 TP 또는 SL 주문 하나만 취소하고 sibling을 자동 취소하지 않는다
- LONG TP: `markPrice >= triggerPrice`
- LONG SL: `markPrice <= triggerPrice`
- SHORT TP: `markPrice <= triggerPrice`
- SHORT SL: `markPrice >= triggerPrice`
- TP/SL 체결 가격은 market close와 같은 taker 성격으로 최신 체결가를 사용하고, 트리거 기준은 mark price다
- 같은 market event 안에서는 pending limit 체결, liquidation 평가, TP/SL 평가 순서로 처리한다. liquidation이 먼저 포지션을 종료하면 TP/SL은 더 이상 평가하지 않는다.
- TP/SL 종료도 조건부 close order fill로 처리하며, 같은 포지션의 pending close 주문은 모두 OCO-aware cap reconciliation 대상이 된다
- TP/SL 종료 이벤트는 관련 저장 transaction이 commit된 이후 `POSITION_TAKE_PROFIT` 또는 `POSITION_STOP_LOSS`로 발행한다

### 부분 체결

MVP 1차 구현은 부분 체결을 지원하지 않는다.
주문은 `전량 체결` 또는 `미체결`로 처리한다.

이 단순화는 구현 복잡도를 줄이기 위한 결정이다.

주문 체결 SSE 이벤트는 관련 계정/주문/포지션 저장 transaction이 commit된 이후에만 발행한다.
rollback된 체결 또는 포지션 충돌은 사용자에게 보이는 체결 이벤트를 만들지 않는다.

## 수수료 규칙

수수료는 체결 순간 명목가치에 비례해 계산한다.

### 기본 식

`fee = executedNotional * feeRate`

### 예시

- `BTCUSDT`
- 수량 `0.2`
- 체결가 `10000`
- 명목가치 `2000`

`maker fee = 2000 * 0.00015 = 0.3 USDT`

`taker fee = 2000 * 0.0005 = 1.0 USDT`

### 수수료 차감 시점

- 주문 체결 직후 차감한다
- 체결 히스토리와 fee ledger에 기록한다

## 포지션 생성 규칙

### 포지션 방향

- `LONG`: 가격 상승 시 수익
- `SHORT`: 가격 하락 시 수익

### 포지션 합치기

MVP 1차는 동일 심볼 + 동일 방향 + 동일 마진 모드 포지션을 하나의 집계 포지션으로 합친다.

즉, `BTCUSDT LONG ISOLATED`를 여러 번 열면 평균 진입가 기반 단일 포지션으로 관리한다.

## 손익 계산 규칙

### LONG 미실현 손익

`unrealizedPnl = (markPrice - entryPrice) * quantity`

입력은 현재 포지션의 평균 `entryPrice`, 열린 `quantity`, 평가 기준 `markPrice`다. 백엔드는
조회/청산 평가에 사용하고, 프론트엔드는 선택 심볼의 실시간 표시를 위해 같은 식을 표시
전용으로 사용할 수 있다. USDT 단위 double 값이며 별도 반올림 없이 응답/표시 계층의 숫자
포맷터가 자릿수를 정한다.

### SHORT 미실현 손익

`unrealizedPnl = (entryPrice - markPrice) * quantity`

입력/기준 가격/반올림 책임은 LONG 미실현 손익과 동일하다.

### ROE

`roi = unrealizedPnl / margin`

`margin`은 열린 포지션의 초기 증거금(`entryPrice * quantity / leverage`)이다. 프론트엔드
표시 전용 중복 계산에서는 API의 `margin` 값을 입력으로 사용한다. `margin`이 0 이하이거나
non-finite이면 `0`으로 표시해 `NaN`을 노출하지 않는다.

### Close amount

`closeAmount = accumulatedClosedQuantity`

`accumulatedClosedQuantity`는 부분/전체 종료 체결이 누적한 종료 수량이다. pending 종료 주문
수량과 `closeableQuantity`는 이 값에 더하지 않는다. 새 포지션의 값은 `0`이며, 부분 종료
체결 이후 남은 포지션 응답에서는 누적 종료 수량을 유지한다.

### 실현 손익

포지션을 부분 또는 전체 종료할 때 종료 수량 기준으로 계산한다.

`grossRealizedPnl = closeSidePnl`

`eventNetRealizedPnl = closeSidePnl - currentCloseFee`

`positionNetRealizedPnl = accumulatedGrossRealizedPnl - accumulatedOpenFee - accumulatedCloseFee - accumulatedFundingCost`

API의 포지션 히스토리 `realizedPnl`은 최종 순손익인 `positionNetRealizedPnl`을 의미한다.
gross PnL은 `grossRealizedPnl`, fee는 `openFee`, `closeFee`, `totalFee`, funding은 `fundingCost`, 순손익은 `netRealizedPnl`로 별도 저장한다.

부분 종료는 열린 포지션에 누적 종료 수량, 누적 종료 명목가치, 누적 실현 손익을 보존한다.
오픈 주문 체결 수수료는 포지션의 `accumulatedOpenFee`에 누적하고, 종료 수수료는 `accumulatedCloseFee`에 누적한다.
funding 정산 원장은 `accumulatedFundingCost`를 사용하며, funding ledger가 실제 정산을 수행하지 않는 MVP 구간에서는 기본값 `0`으로 유지한다.
포지션 히스토리는 포지션이 완전히 종료되는 순간에만 생성한다.

포지션 히스토리 ROI는 아래 식으로 계산한다.

`positionRoi = positionNetRealizedPnl / originalInitialMargin`

여기서 `originalInitialMargin = averageEntryPrice * originalQuantity / leverage`다.
강제 청산도 포지션이 종료된 경우이므로 동일하게 포지션 히스토리에 남기되 종료 사유만 `LIQUIDATION`으로 구분한다.
TP/SL로 종료된 포지션은 종료 사유를 각각 `TAKE_PROFIT`, `STOP_LOSS`로 구분한다.

## 증거금 규칙

### 명목가치

`notional = entryPrice * quantity`

### 격리 마진 초기 증거금

`initialMargin = notional / leverage`

격리 마진에서는 포지션별로 이 증거금을 따로 잡아둔다.

### 교차 마진 초기 증거금

교차 마진도 시작 시점에는 같은 식으로 필요한 최소 증거금을 계산하지만,
실제 리스크 평가는 계정의 가용 자산을 함께 본다.

즉, 교차 마진은 "포지션별 진입 필요 증거금"과 "계정 전체 청산 리스크"를 분리해서 본다.

## 청산 규칙 초안

### 공통 원칙

- 청산 평가는 `mark price` 기준이다
- `maintenance margin`을 밑돌면 청산 대상으로 본다
- 유지 증거금률은 MVP 1차에서 심볼 공통 고정값으로 시작한다
- 청산 체결 이벤트는 DB transaction commit 이후에만 발행한다

### 유지 증거금률

MVP 1차 고정값:

- maintenance margin rate: `0.5%`

### 격리 마진 청산

격리 마진은 포지션 단위로 평가한다.
해당 포지션의 격리 증거금과 손익만 보고 청산 여부를 판단한다.

### 교차 마진 청산

교차 마진은 계정 단위로 평가한다.
같은 계정의 교차 마진 포지션들과 가용 잔고를 함께 보고 청산 여부를 판단한다.

MVP 1차는 아래 순서로 단순화한다.

1. 교차 마진 포지션들의 총 미실현 손익을 계산한다
2. 계정의 사용 가능 잔고와 합산한다
3. 총 유지 증거금 요구치를 밑돌면 위험 상태로 본다
4. 청산 조건 충족 시 가장 위험도가 높은 포지션부터 종료한다

## funding 규칙

### funding 지급/수취

- funding rate가 양수일 때 long이 지급하고 short가 수취한다
- funding rate가 음수일 때 short가 지급하고 long이 수취한다

### 계산식

`fundingPayment = markPrice * quantity * fundingRate`

실제 정산 금액은 방향에 따라 부호를 다르게 적용한다.

### 정산 시점

- 8시간마다 배치 정산
- 해당 시점에 열려 있는 포지션만 대상
- 심볼별 funding schedule은 `market_symbols`의 메타데이터로 관리한다.
- BTCUSDT/USDT 선물 MVP 기본값은 KST 01:00, 09:00, 17:00이며, `funding_anchor_hour = 1`, `funding_interval_hours = 8`, `funding_time_zone = Asia/Seoul`에서 계산한다.
- 트레이딩 화면은 백엔드가 내려주는 `nextFundingAt`을 기준으로 남은 시간을 표시한다. 프론트엔드는 funding 경계 시각을 직접 계산하지 않고 표시용 카운트다운만 갱신한다.

## 포인트 규칙

포인트는 실현 수익이 확정된 뒤 지급한다.
미실현 손익에는 포인트를 주지 않는다.

### 지급 원칙

- 순실현 손익이 양수일 때만 지급
- 손실 또는 0이면 지급 없음
- 지급 단위는 거래 단위가 아니라 `포지션 종료 이벤트` 기준

### MVP 1차 지급 규칙

- `순실현손익 < 10000 USDT`: `0 point`
- 순실현손익 `10000 USDT`마다 `5 points`
- 예: `10000 USDT`는 `5 points`, `20000 USDT`는 `10 points`

포인트 계산은 `floor(순실현손익 / 10000) * 5`로 한다.

## 상점 규칙

### 원칙

- 상점 아이템은 투자 성능에 직접 영향 주지 않는다
- MVP 상점 아이템은 커피 교환권으로 시작한다
- 포인트는 현금으로 환전되지 않는다
- 포인트는 정수 단위로만 적립, 차감, 환불한다
- 상점 상품은 정적 코드가 아니라 DB 운영 데이터로 관리한다

### MVP 1차 상품

- 커피 교환권
- 관리 방식:
  migration/bootstrap/data-admin
- 관리자 상품 CRUD UI/API:
  MVP 범위 밖

### 판매 가능성

판매 가능 여부는 별도 `sellable` 플래그가 아니라 아래 조건으로 계산한다.

- `active = true`
- `total_stock = null`이면 무제한 재고, 유한 재고 상품이면 `sold_quantity < total_stock`
- `per_member_purchase_limit = null`이면 유저별 제한 없음, 제한이 있으면 해당 유저의 `purchase_count < per_member_purchase_limit`
- 사용자의 포인트 잔액이 상품 가격 이상

`sold_quantity`는 item-level 재고 소진 수량이다.
유저별 제한은 `purchase_count`로 추적하며, `PENDING`과 `SENT` 요청만 카운트한다.
`CANCELLED_REFUNDED` 요청은 재고와 유저 구매 카운트를 정확히 한 번 복구한다.
판매 가능성 검증과 재고/구매 카운트/포인트 차감은 같은 트랜잭션에서 수행해 검증 시점과 저장 시점이 갈라지지 않게 한다.

### 교환권 신청

- 사용자는 `/shop`에서 커피 교환권을 선택하고 휴대폰 번호를 입력한다.
- 휴대폰 번호는 숫자와 하이픈만 허용하며, 서버에서 10~11자리 숫자로 정규화한다.
- 유효하지 않은 번호, 포인트 부족, 품절, 비활성 상품, 유저별 구매 제한 초과는 요청을 만들지 않고 포인트/재고/이력을 변경하지 않는다.
- 성공 요청은 한 트랜잭션 안에서 상품 재고 예약, 유저 구매 카운트 증가, 포인트 차감, 차감 이력 생성, `PENDING` 요청 생성을 수행한다.
- 요청 시점의 상품 code/name/price/point amount는 redemption request에 스냅샷으로 저장한다.

### 관리자 처리

- `PENDING -> SENT`:
  관리자가 발송 완료 처리한다. 이미 발송된 요청은 환불할 수 없다.
- `PENDING -> CANCELLED_REFUNDED`:
  포인트를 환불하고, 환불 이력을 남기며, 상품 `sold_quantity`와 유저 `purchase_count`를 guarded decrement로 복구한다.
- guarded decrement는 현재 값이 0이면 더 줄이지 않아 음수 재고와 음수 구매 카운트를 방지한다.
- 중복 취소는 `PENDING -> CANCELLED_REFUNDED` 전이가 한 번만 성공하기 때문에 포인트/재고/구매 카운트를 두 번 복구하지 않는다.

### 알림

- 새 교환권 요청 생성 후 DB commit 이후 SMTP 알림을 보낸다.
- 관리자 수신자는 `coin.reward.notification.admin-email` 설정값이며 기본값은 `gudwns1812@naver.com`이다.
- SMTP 실패는 요청을 롤백하지 않고 request id와 수신자를 포함해 로그로 남긴다.
- Discord 등 추가 알림 채널은 같은 notification boundary에 구현한다.

## 데이터 저장 규칙

아래 기록은 별도 이력으로 남겨야 한다.

- 주문 생성
- 주문 취소
- 주문 체결
- 수수료 차감
- funding 정산
- 포지션 종료
- 포인트 적립
- 상점 구매

## 프론트 계산과 서버 계산의 책임 분리

### 프론트에서 하는 것

- 예상 수수료 계산
- 예상 증거금 계산
- 예상 청산가 미리보기

### 서버가 최종 결정하는 것

- 실제 체결 여부
- maker/taker 판정
- 실제 수수료
- 실제 손익
- 실제 청산
- 실제 포인트 적립

즉, 프론트 계산은 안내용이고 서버 계산이 최종 진실이다.

## 바로 구현으로 옮길 최소 인터페이스

### 주문 생성 입력

```ts
type CreateOrderRequest = {
  symbol: "BTCUSDT" | "ETHUSDT";
  positionSide: "LONG" | "SHORT";
  orderType: "MARKET" | "LIMIT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  limitPrice?: number;
};
```

### 주문 계산 응답

```ts
type OrderPreview = {
  estimatedEntryPrice: number;
  estimatedFee: number;
  estimatedInitialMargin: number;
  estimatedLiquidationPrice: number | null;
  feeType: "MAKER" | "TAKER";
};
```

### 포지션 스냅샷

```ts
type PositionSnapshot = {
  symbol: "BTCUSDT" | "ETHUSDT";
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  entryPrice: number;
  markPrice: number;
  liquidationPrice: number | null;
  unrealizedPnl: number;
  realizedPnl: number;
  accumulatedClosedQuantity: number;
  pendingCloseQuantity: number;
  closeableQuantity: number;
  takeProfitPrice: number | null;
  stopLossPrice: number | null;
};
```

## Position card live mark-to-market and close amount rules

- **Live display formula**: for the currently selected market only, the frontend may duplicate the backend read-time mark-to-market display formula until the next authoritative refresh. LONG unrealized PnL is `(markPrice - entryPrice) * quantity`; SHORT unrealized PnL is `(entryPrice - markPrice) * quantity`; ROE is `unrealizedPnl / margin`.
- **Price source**: frontend live display uses the selected market SSE `markPrice`. Backend persisted/account state remains authoritative and `GET /api/futures/positions/me` still mark-to-markets from the backend market gateway on read.
- **Edge cases**: non-finite mark price, non-finite margin, or zero/negative margin must not render `NaN`; ROE falls back to `0` for display.
- **Close amount**: the `Close amount` label means accumulated closed quantity (`accumulatedClosedQuantity`) for the still-open position. It starts at `0` for a new position and increases only after partial close execution.
- **Pending and closeable quantity**: `pendingCloseQuantity` is the sum of pending close orders, and `closeableQuantity` is retained as compatibility data. Neither field is displayed as `Close amount`.
- **Close order acceptance and reconciliation**: a new close request is validated against held position quantity, not closeable quantity. The backend may accept a new close order before reconciling pending close caps; after submission, pending close orders are reduced or cancelled so total pending close quantity does not exceed the remaining held quantity. LONG close reconciliation reduces/cancels higher limit-price orders first; SHORT reduces/cancels lower limit-price orders first; ties reduce newer orders before older orders.
- **TP/SL editing**: position-level TP/SL values are displayed on the position card, but inputs are hidden by default and opened via the `Position TP/SL` edit action. Save and clear use `/api/futures/positions/tpsl`, whose persistence source is pending conditional close orders, not `open_positions` TP/SL columns.
