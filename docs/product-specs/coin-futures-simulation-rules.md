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
- 동일 계정/심볼/방향/마진 모드의 pending 종료 주문 수량 합계는 현재 열린 포지션 수량을 초과할 수 없다
- 초과 시 새 주문, market close 이후 남은 주문, pending 종료 주문 체결 이후 남은 주문, liquidation 이후 남은 주문을 모두 포함해 실행 가능성이 낮은 종료 주문부터 줄이거나 취소한다
  - LONG 종료 주문은 현재가보다 더 높은 지정가가 덜 실행되기 쉬우므로 지정가 내림차순으로 줄인다. 예: 현재가 `105`, close long `112`, `108`이 있으면 `112`를 먼저 줄인다.
  - SHORT 종료 주문은 현재가보다 더 낮은 지정가가 덜 실행되기 쉬우므로 지정가 오름차순으로 줄인다. 예: 현재가 `95`, close short `88`, `92`가 있으면 `88`을 먼저 줄인다.
  - 같은 지정가에서는 기존 주문 우선권을 보존하기 위해 더 나중에 생성된 주문을 먼저 줄이고, 생성 시각까지 같으면 `orderId` 오름차순으로 결정한다.
  - 일부만 줄어든 주문은 `PENDING` 상태로 수량을 조정하고, 전량 줄어든 주문은 `CANCELLED`로 남겨 주문 이력을 보존한다
- 이 cap은 포지션을 늘리는 `OPEN_POSITION` 주문에는 적용하지 않는다
- 포지션 API는 같은 포지션의 pending 종료 주문 합계인 `pendingCloseQuantity`와 `closeableQuantity = max(0, quantity - pendingCloseQuantity)`를 내려준다

포지션 종료, pending 종료 주문 체결, 청산 종료는 열린 포지션의 `version`을 조건으로 하는 낙관적 잠금 mutation이다.
이미 다른 요청이 포지션을 변경했다면 계정 정산, 포지션 이력, 리워드 지급, 응답용 체결 이벤트 발행 전에 충돌로 중단한다.

### 포지션 TP/SL

열린 포지션은 선택 값인 `takeProfitPrice`, `stopLossPrice`를 가질 수 있다.
TP/SL 편집 API는 최신 mark price를 읽고, 저장 직후 이미 발동되는 값을 거절한다.

- LONG TP: `markPrice >= takeProfitPrice`
- LONG SL: `markPrice <= stopLossPrice`
- SHORT TP: `markPrice <= takeProfitPrice`
- SHORT SL: `markPrice >= stopLossPrice`
- null TP/SL은 평가하지 않는다
- TP/SL 체결 가격은 market close와 같은 taker 성격으로 최신 체결가를 사용하고, 트리거 기준은 mark price다
- 같은 market event 안에서는 pending limit 체결, liquidation 평가, TP/SL 평가 순서로 처리한다. liquidation이 먼저 포지션을 종료하면 TP/SL은 더 이상 평가하지 않는다.
- TP/SL 종료도 포지션 전체 종료로 처리하며, 같은 포지션의 pending close 주문은 모두 cap reconciliation 대상이 된다
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

### SHORT 미실현 손익

`unrealizedPnl = (entryPrice - markPrice) * quantity`

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
- cosmetic 또는 profile 성격으로 시작한다
- 포인트는 현금으로 환전되지 않는다

### MVP 1차 아이템 예시

- 닉네임 뱃지
- 프로필 프레임
- 대시보드 테마
- 칭호

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
  pendingCloseQuantity: number;
  closeableQuantity: number;
  takeProfitPrice: number | null;
  stopLossPrice: number | null;
};
```
