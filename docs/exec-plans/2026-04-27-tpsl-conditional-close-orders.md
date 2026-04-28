# TP/SL 조건부 청산 주문 재설계

이 계획서는 `PLANS.md` 규칙을 따른다.

## 목적 / 큰 그림

TP/SL을 포지션에 붙은 숫자 상태가 아니라 하나의 주문으로 관리한다. 사용자는 TP/SL을 open orders/history에서 보고, 취소하고, 체결 이력으로 이해할 수 있어야 한다. 또한 TP와 SL이 모두 2BTC 전체 수량으로 걸려 있어도 실제 청산 예약 수량은 4BTC가 아니라 2BTC로 계산되어야 한다.

## 진행 현황

- [x] 계획 초안 작성
- [x] Architect 검토 승인
- [x] Critic 검토 승인
- [ ] 사용자 승인
- [ ] 구현
- [ ] 테스트
- [ ] 문서 반영
- [ ] 작업 종료 처리

## 의사결정 기록

### ADR: TP/SL은 조건부 청산 주문이다

Decision: TP/SL을 pending conditional `CLOSE_POSITION` order로 모델링한다.

Drivers:

- TP/SL은 사용자가 주문처럼 보고 취소할 수 있어야 한다.
- TP/SL bracket은 close exposure를 중복 예약하면 안 된다.
- 기존 Position TP/SL 편집 UX는 유지하되 저장소와 read model은 order-backed로 바꾼다.

Alternatives considered:

- Position column 기반 유지: 주문 목록/이력/취소/청산 수량 조정과 맞지 않아 거절한다.
- 별도 TP/SL 테이블 추가: order lifecycle을 중복하므로 거절한다.
- 전체 trigger/stop-limit order engine 도입: 현재 범위를 넘어가므로 거절한다.

Consequences:

- `futures_orders`에 trigger metadata와 OCO group 개념이 추가된다.
- 기존 `open_positions.take_profit_price`, `open_positions.stop_loss_price`는 legacy 호환 컬럼으로 남길 수 있으나, application/domain read/write/trigger path에서는 사용하지 않는다.
- closeable quantity 공식이 OCO-aware로 바뀌며 제품 스펙과 설계 문서에 반드시 기록되어야 한다.

## 범위

이번에 하는 것(in scope):

- TP/SL을 조건부 `CLOSE_POSITION` 주문으로 저장한다.
- TP/SL save/clear API는 유지하되 내부 구현을 주문 생성/취소로 바꾼다.
- open orders/history와 position summary를 order-backed read model로 바꾼다.
- OCO-aware close exposure 공식을 적용한다.
- product/design docs에 공식과 계약을 기록한다.

이번에 하지 않는 것(out of scope):

- full exchange-style stop-limit/trigger order engine
- 별도 `reduceOnly` 컬럼 도입
- 기존 position TP/SL 컬럼의 즉시 삭제

## 요구 사항 요약

- TP/SL 주문은 `orderPurpose = CLOSE_POSITION`, `status = PENDING`, `triggerPrice != null`, `triggerType != null`, `triggerSource = MARK_PRICE`, `limitPrice = null`, `orderType = MARKET` 계약을 따른다.
- ordinary order는 trigger metadata가 모두 null이어야 한다.
- 같은 `memberId + symbol + positionSide + marginMode + triggerType`에는 active pending TP/SL이 하나만 존재해야 한다.
- TP/SL 두 sibling이 함께 저장되면 같은 `ocoGroupId`를 공유한다.
- 단일 TP 또는 SL은 `ocoGroupId = null`을 허용하되 uniqueness invariant로 중복을 막는다.
- 수동 취소는 선택한 TP 또는 SL 하나만 취소한다.
- OCO sibling 자동 취소는 sibling 체결 시에만 수행한다.
- 포지션 종료 지정가 모달, open 주문, order book 기반 가격 입력은 모달/패널이 열리거나 사용자가 order book 가격을 선택한 순간의 가격을 한 번만 price 칸에 채운다. 이후 mark price, latest price, order book 업데이트가 들어와도 사용자가 입력 중인 price 값을 자동으로 덮어쓰지 않는다. TP/SL 신규 편집 입력은 mark price를 기본값으로 채우지 않고 빈 값에서 시작하며, 기존 TP/SL이 있을 때만 기존 trigger price를 한 번 채운다.

## 핵심 공식

Scope: `memberId + symbol + positionSide + marginMode`

```text
effectivePendingCloseQty =
  sum(quantity of pending CLOSE_POSITION orders where ocoGroupId is null)
  + sum(max(quantity) per ocoGroupId among pending CLOSE_POSITION orders where ocoGroupId is not null)
```

`FILLED`, `CANCELLED` 주문은 제외한다. TP 2BTC와 SL 2BTC가 같은 OCO group이면 pending close exposure는 4BTC가 아니라 2BTC다.

## Trigger 공식

Trigger source는 mark price다.

- LONG TP: `markPrice >= triggerPrice`
- LONG SL: `markPrice <= triggerPrice`
- SHORT TP: `markPrice <= triggerPrice`
- SHORT SL: `markPrice >= triggerPrice`

Execution price는 last price를 사용하고, 수수료는 taker fee를 사용한다.

## Reconciliation 정책

Pending close exposure가 현재 포지션 수량을 초과하면:

1. manual close order를 TP/SL bucket보다 우선 보존한다.
2. TP/SL OCO bucket을 먼저 줄이거나 취소한다.
3. 같은 OCO group의 sibling quantity는 같은 조정 수량으로 맞춘다.
4. 남은 초과분은 deterministic least-likely/latest ordering으로 조정한다.
5. TP/SL 저장, 부분 청산, manual close 체결, conditional fill, position shrink 뒤에는 reconciliation을 다시 실행한다.

## 작업 계획

1. `futures_orders`에 `trigger_price`, `trigger_type`, `trigger_source`, `oco_group_id`를 추가한다.
2. order domain validation과 persistence DTO/entity를 trigger metadata에 맞게 확장한다.
3. `UpdatePositionTpslService`를 position mutation에서 conditional close order create/cancel command로 바꾼다.
4. 기존 position-field TP/SL scanner를 제거하고 order-backed conditional processor를 추가한다.
5. market update 순서는 현재처럼 ordinary pending limit fill -> liquidation -> conditional TP/SL order로 고정한다.
6. `PendingOrderFillProcessor`가 conditional market TP/SL을 ordinary limit fill로 처리하지 않게 필터링한다.
7. `PendingCloseOrderCapReconciler`를 OCO bucket 공식과 manual close 보호 정책으로 바꾼다.
8. position summary와 frontend DTO/UI를 order-backed TP/SL로 바꾼다.
9. open orders/history에 `TP Close`, `SL Close` 라벨과 trigger price를 표시한다.
10. price input 초기값 정책을 고정한다. 포지션 종료 모달은 열릴 때 mark price를 한 번만 채우고, 지정가/open 주문과 order book 선택 가격도 사용자가 입력을 시작한 뒤 realtime price/order book 업데이트로 자동 변경하지 않는다. TP/SL 신규 편집 입력은 mark price 기본값 없이 빈 값으로 시작하고 기존 TP/SL이 있으면 기존 trigger price만 한 번 채운다.
11. product specs, db schema docs, backend design docs에 계약과 공식을 기록한다.

## 수용 기준(테스트 가능한 형태)

- Saving TP/SL creates pending conditional `CLOSE_POSITION` orders.
- Clearing TP/SL cancels pending TP/SL conditional close orders for the same position scope.
- Position summary derives TP/SL prices from orders, not position columns.
- Full-size TP and SL siblings count as one position-sized close exposure.
- Manual close orders are protected ahead of TP/SL buckets during reconciliation.
- Conditional TP/SL uses mark price for trigger evaluation and last price for execution.
- Triggered TP/SL closes through the existing close finalizer and cancels its OCO sibling.
- Frontend save/clear refreshes position, open orders, and order history.
- Open orders/history render `TP Close` and `SL Close`.
- Price inputs are snapshot-initialized only once and are not overwritten by later mark/latest/order book updates while the user is editing. New TP/SL fields are the exception: they start empty unless an existing trigger price is being edited.
- Docs record trigger formulas, OCO exposure formula, manual close protection, and legacy column status.

## 검증 절차

실행 명령:

```bash
cd backend && ./gradlew test
cd frontend && npm run lint
cd frontend && npm test
git diff --check
```

브라우저 검증:

1. ETHUSDT 시장을 연다.
2. LONG 포지션을 연다.
3. Position TP/SL 편집기에서 TP와 SL을 저장한다.
4. `TP Close`, `SL Close` pending order가 보이는지 확인한다.
5. TP/SL bracket이 closeable quantity를 두 배로 잠그지 않는지 확인한다.
6. manual close order를 추가해도 manual close가 우선 보존되고 TP/SL bucket이 조정되는지 확인한다.
7. TP 또는 SL을 trigger해 sibling cancellation, position refresh, open orders/history 반영을 확인한다.
8. 포지션 종료 모달과 지정가/open 주문 price 칸이 최초 가격으로 한 번만 채워지고, 이후 mark price/latest price/order book 갱신으로 자동 변경되지 않는지 확인한다. TP/SL 신규 입력은 기본값 없이 비어 있고 기존 TP/SL 편집 때만 기존 trigger price가 채워지는지 확인한다.

## 실행 핸드오프

권장 기본값은 `$ralph`다. 이 변경은 persistence, domain, processor, reconciler, read model, frontend, docs, tests를 모두 건드리므로 한 명의 owner가 일관성을 지키는 편이 안전하다.

`$team`을 쓴다면 lane ownership을 좁게 나눈다.

- Backend persistence/domain executor: high reasoning
- Backend processor/reconciler executor: high reasoning
- Frontend editor/orders executor: medium reasoning
- Docs writer: medium reasoning
- Verifier/test-engineer: high reasoning

Suggested launch:

```text
$team implement TP/SL as conditional close orders using docs/exec-plans/2026-04-27-tpsl-conditional-close-orders.md
```
