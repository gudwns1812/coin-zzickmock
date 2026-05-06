# Open Orders marketable 주문 수정 즉시 체결

## 목적 / 큰 그림
Open Orders 주문 수정에서 사용자가 즉시 체결 가능한 지정가로 가격을 바꾸면 오류로 막지 않고 빠른 체결 의도로 처리한다. 완료 후 사용자는 기존 주문 row identity를 유지한 채 marketable edit를 `TAKER` 체결로 마무리할 수 있다.

## 진행 현황
- [x] 계획 초안 작성
- [x] 새 브랜치 생성 및 branch policy 확인
- [x] 구현
- [x] 테스트
- [ ] PR 생성
- [ ] CodeRabbit review 및 피드백 반영 루프
- [ ] 작업 종료 처리

## 놀라움과 발견
- 기존 주문 수정 계획은 marketable edit를 hidden fill flow로 보아 거절했지만, 사용 의도 관점에서는 빠른 체결이 더 자연스럽다.
- 현재 backend에는 기존 row를 `FILLED`로 claim하는 repository 경로와 open/close fill side effect 협력 객체가 이미 있다.
- `docs/exec-plans/README.md`가 가리키는 `PLANS.md`, `CI_WORKFLOW.md`는 현재 루트에 없어 읽을 수 없었다.

## 의사결정 기록
- Marketable edit는 cancel-replace로 새 주문을 만들지 않고 기존 pending order row를 수정/claim한다.
- Marketable edit의 체결은 immediate limit execution이므로 `TAKER`, execution price는 최신 체결가다.
- Non-marketable edit는 기존처럼 `PENDING`/`MAKER`로 유지한다.
- TP/SL 조건부 주문은 기존 포지션 TP/SL editor가 담당하므로 이 기능에서 계속 제외한다.

## 범위
- 이번에 하는 것(in scope): backend modify flow, open/close marketable edit fill, frontend success copy/test adjustment, 제품 명세와 계획 갱신, 검증, PR/CodeRabbit loop.
- 이번에 하지 않는 것(out of scope): 수량/레버리지/마진 모드/방향/목적 수정, TP/SL 조건부 주문 수정, DB schema 변경, 새 dependency 추가.
- 후속 작업(선택): UI에서 marketable edit가 즉시 체결될 수 있음을 더 명확히 보여주는 copy/preview 개선.

## 요구 사항 요약
- 기능 요구 사항: pending non-conditional LIMIT 주문의 marketable price edit는 즉시 taker 체결한다.
- 비기능 요구 사항: layer boundary 준수, 기존 row identity 유지, concurrent pending fill 안전성 유지, no new dependency.

## 맥락과 길잡이
- 관련 문서: `AGENTS.md`, `README.md`, `ARCHITECTURE.md`, `BACKEND.md`, backend design 01/02/03/04/05/06/09, `FRONTEND.md`, `frontend/README.md`, UI design README, `docs/product-specs/README.md`, `coin-futures-simulation-rules.md`, `coin-futures-screen-spec.md`, `docs/process/branch-and-pr-rules.md`.
- 관련 코드 경로: `ModifyOrderService`, `OrderRepository`, `OrderPersistenceRepository`, `FuturesOrderEntityRepository`, `FilledOpenOrderApplier`, `PendingOrderFillProcessor`, `ClosePositionService`, `PositionCloseFinalizer`, `futures-order-edit`.
- 선행 조건: 브랜치 `feat/marketable-order-edit-fill`; `npm run check:branch -- feat/marketable-order-edit-fill` 및 현재 브랜치 검사 통과.

## 문서 원문 대조표
| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| backend application/domain | `BACKEND.md`, `01-architecture-foundations.md`, `02-package-and-wiring.md`, `03-application-and-providers.md`, `04-domain-modeling-rules.md` | feature-first, application use case boundary, domain policy 재사용, service 간 직접 주입 금지 | `ModifyOrderService`에서 `OrderPlacementPolicy`, existing applier/finalizer 협력 객체를 활용 | controller에서 repository 직접 수정, 새 pass-through interface 추가, domain 규칙 중복 | targeted service tests, architectureLint |
| backend persistence | `06-persistence-rules.md` | update/claim 의도를 repository 계약에 드러내고 guarded atomic update 사용 | 기존 guarded update/claim 계약을 확장 또는 조합 | generic save로 status overwrite, schema 불필요 변경 | repository contract tests, gradle check |
| backend exception/testing | `09-exception-rules.md`, `05-testing-and-lint.md` | 비즈니스 실패는 `CoreException`, tests by layer | invalid/stale claim은 `INVALID_REQUEST`; H2 persistence contract로 race guard 고정 | custom detail message, broad catch | targeted tests, check |
| frontend UI/state | `FRONTEND.md`, `frontend/README.md`, `ui-design/README.md` | client interaction만 client component, response.ok 확인, 공용 패턴 유지 | 성공 copy/test만 필요한 범위에서 조정 | 새 fetch 패턴 확산, Zustand server state 추가 | frontend node test/build |
| product spec | `docs/product-specs/README.md`, `coin-futures-simulation-rules.md`, `coin-futures-screen-spec.md` | 주문/체결 규칙 변경은 명세 갱신 | pending limit edit subsection을 marketable taker fill로 변경 | 코드만 바꾸고 제품 의미 미문서화 | docs diff review |
| branch/PR | `docs/process/branch-and-pr-rules.md` | `<type>/<kebab-case-summary>`, codex prefix 금지 | `feat/marketable-order-edit-fill` | 외부 tool default branch/PR prefix 사용 | `npm run check:branch`, PR title prefix |

대조표 점검:
- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 설계 문서를 우선한다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인한다.

## 작업 계획
1. 기존 reject semantics를 marketable edit fill semantics로 바꾸는 service/repository 설계를 적용한다.
2. Open fill은 `FilledOpenOrderApplier`, close fill은 close finalization/cap cleanup 경로를 재사용한다.
3. Unit/persistence/transaction regression tests를 marketable fill 기준으로 갱신한다.
4. Product spec과 frontend success copy/test를 갱신한다.
5. targeted tests, architectureLint, backend check, frontend build를 실행한다.
6. Commit, PR 생성, CodeRabbit review, 피드백 반영 반복을 수행한다.

## 수용 기준(테스트 가능한 형태)
- marketable open limit edit는 같은 order id를 `FILLED`/`TAKER`로 만들고 포지션/계정 상태를 갱신한다.
- marketable close limit edit는 같은 order id를 `FILLED`/`TAKER`로 만들고 포지션 close side effect를 적용한다.
- non-marketable edit는 pending maker 상태를 유지한다.
- invalid/non-pending/conditional/different-member edit는 실패한다.
- 제품 명세와 테스트가 새 semantics를 원문으로 고정한다.
- CodeRabbit review에서 더 이상 actionable feedback이 없다.

## 위험과 완화
- 위험: marketable edit가 open/close 회계를 중복하거나 누락한다.
  - 예방: 기존 applier/finalizer를 재사용한다.
  - 완화: open/close service tests로 계정/포지션/order 상태를 같이 검증한다.
  - 복구: 변경 범위를 `ModifyOrderService`와 repository helper로 되돌릴 수 있게 유지한다.
- 위험: pending fill loop와 동시 수정이 priority를 깨뜨린다.
  - 예방: account mutation lock과 guarded claim을 유지한다.
  - 완화: stale expected limit claim test를 유지/추가한다.
  - 복구: claim 실패 시 `INVALID_REQUEST`로 종료하고 재시도 가능하게 한다.
- 위험: UI가 filled result를 오류로 오해한다.
  - 예방: result status 기반 copy를 허용하고 behavior test를 갱신한다.
  - 완화: 성공 후 `router.refresh()`는 그대로 유지한다.
  - 복구: copy만 별도 수정 가능하다.

## 검증 절차
- 실행 명령: `.omx/plans/test-spec-marketable-order-edit-fill.md`의 backend/frontend commands.
- 기대 결과: all pass, CodeRabbit no actionable feedback.
- 실패 시 확인할 것: service dependency cycle, repository guarded query predicate, transaction rollback/commit behavior, frontend copy expectation.

## 산출물과 메모
- Autopilot context: `.omx/context/marketable-order-edit-fill-20260506T001324Z.md`
- PRD: `.omx/plans/prd-marketable-order-edit-fill.md`
- Test spec: `.omx/plans/test-spec-marketable-order-edit-fill.md`

## 구현 및 검증 메모
- `MarketableEditedOrderFiller`가 수정된 pending limit 주문을 guarded claim 후 open fill applier 또는 close finalizer로 연결한다.
- `ModifyOrderService`는 update 후 fresh market을 재조회하고, marketable이면 `FILLED`/`TAKER` 결과를 반환한다.
- Frontend success copy는 response `status`가 `FILLED`이면 즉시 체결로 표시한다.
- 통과한 검증: targeted backend service/transaction/repository tests, frontend node tests, backend `architectureLint`, backend `check`, frontend `build`.
