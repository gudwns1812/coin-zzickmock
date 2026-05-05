# Open Orders 주문 수정 기능

## 목적 / 큰 그림
Open Orders에서 대기 지정가 주문을 취소 후 재입력하지 않고 가격만 수정할 수 있게 한다. 완료 후 사용자는 주문 행의 수정 버튼으로 모달을 열고 새 limit price를 저장할 수 있다.

## 진행 현황
- [x] 계획 초안 작성
- [x] Autopilot ralplan 산출물 작성
- [x] 구현
- [x] 테스트
- [ ] review 스킬 기반 검토 확인
- [ ] 작업 종료 처리(완료 판단 및 completed 이동)

## 놀라움과 발견
- 기존 Open Orders UI는 `CancelOrderButton`만 별도 client component로 분리되어 있다.
- pending close limit order의 estimated fee는 제품 규칙에 따라 0이어야 한다.
- 수정으로 즉시 체결되는 가격을 허용하면 open/close fill 회계까지 동기 처리해야 하므로 이번 범위에서는 거절한다.

## 의사결정 기록
- 주문 수정 범위는 pending non-conditional LIMIT order의 `limitPrice`만으로 제한한다.
- TP/SL 조건부 주문은 기존 포지션 TP/SL editor가 담당하므로 Open Orders edit에서 거절한다.
- 즉시 체결 가능한 가격 변경은 오류로 거절해 pending order edit가 hidden fill flow가 되지 않게 한다.

## 범위
- 이번에 하는 것(in scope): backend modify API/service/repository, frontend edit modal/button, 제품 명세 업데이트, 테스트/검증.
- 이번에 하지 않는 것(out of scope): 수량/레버리지/마진 모드 수정, TP/SL 수정, DB schema 변경, 즉시 체결형 주문 수정.
- 후속 작업(선택): cancel-replace semantics 또는 marketable edit 즉시 체결 지원.

## 요구 사항 요약
- 기능 요구 사항: Open Orders 주문 수정 버튼, 가격 입력 모달, backend price update.
- 비기능 요구 사항: layer boundary 준수, no new dependency, refresh/toast UX, 안전한 validation.

## 맥락과 길잡이
- 관련 문서: `BACKEND.md`, backend design 01/02/03/04/05/06/09, `FRONTEND.md`, `frontend/README.md`, UI design README, `docs/product-specs/coin-futures-simulation-rules.md`.
- 관련 코드 경로: `feature/order`, `MarketDetailRealtimeView.tsx`, `futures-client-api.ts`.
- 선행 조건: 브랜치 `feat/open-order-edit` 생성 및 branch policy 통과.

## 문서 원문 대조표
| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| backend application/domain | `BACKEND.md`, `01-architecture-foundations.md`, `02-package-and-wiring.md`, `03-application-and-providers.md`, `04-domain-modeling-rules.md` | feature-first, web->application, domain state transition method, CoreException | `ModifyOrderService`, command/result, `FuturesOrder.withLimitPrice` | controller에서 repository 직접 수정, domain 없이 raw field 조합 | targeted service/domain tests, architectureLint |
| backend persistence | `06-persistence-rules.md` | 단건 수정은 entity method + dirty checking, bulk는 명시 의도 | `OrderRepository.updatePendingLimitPrice`와 entity update method | generic save로 업데이트, schema 불필요 변경 | service tests, gradle check |
| frontend UI/state | `FRONTEND.md`, `frontend/README.md`, `ui-design/README.md` | client interaction만 client component, response.ok 확인, 공용 Button/Modal 재사용 | `EditOrderButton` client component와 Open Orders row 액션 확장 | 컴포넌트 내부 중복 fetch 패턴 확산, src/features 도입 | frontend node test/build, runtime smoke |
| product spec | `docs/product-specs/README.md`, `coin-futures-simulation-rules.md` | 주문/체결 규칙 변경은 명세 갱신 | pending limit order edit subsection 추가 | 코드만 바꾸고 제품 의미 미문서화 | docs diff review |

## 작업 계획
1. Backend command/result/service/repository/entity/domain method/API를 추가한다.
2. Backend tests로 pending edit와 거절 조건을 고정한다.
3. Frontend client API helper와 `EditOrderButton` 모달을 추가하고 Open Orders row에 배치한다.
4. 제품 명세를 갱신한다.
5. targeted tests, backend check, frontend build를 실행한다.
6. PR 생성, CodeRabbit/direct review, 피드백 반영 루프를 수행한다.

## 수용 기준(테스트 가능한 형태)
- pending non-conditional LIMIT order의 price edit가 성공한다.
- invalid/non-pending/conditional/marketable edit는 실패한다.
- UI에서 “주문 수정” 모달로 새 가격을 저장할 수 있다.
- 취소 버튼은 기존대로 유지된다.
- 검증 명령과 리뷰 게이트가 통과한다.

## 위험과 완화
- 위험: marketable edit의 체결 semantics 혼선.
  - 예방: 이번 범위에서 명시적으로 거절하고 제품 명세에 기록.
  - 완화: 후속 작업으로 cancel-replace/즉시 체결 지원 분리.
  - 복구: API validation 완화와 fill orchestration 추가.
- 위험: pending close fee semantics 손상.
  - 예방: close edit estimated fee 0 테스트.
  - 완화: service 분기와 테스트 유지.
  - 복구: repository 업데이트 값 조정.
- 위험: UI refresh stale.
  - 예방: 성공 후 `router.refresh()`.
  - 완화: toast와 disabled state.
  - 복구: React Query 도입은 후속 큰 작업으로 분리.

## 검증 절차
- 실행 명령: backend targeted tests, `architectureLint`, `check`, frontend tests/build.
- 기대 결과: all pass.
- 실패 시 확인할 것: repository fake contract, QueryDSL generated classes, TypeScript component props.

## 산출물과 메모
- Autopilot context: `.omx/context/open-order-edit-20260505T054000Z.md`
- PRD: `.omx/plans/prd-open-order-edit.md`
- Test spec: `.omx/plans/test-spec-open-order-edit.md`

## Review cycle 1 findings
- BLOCK: `updatePendingLimitPrice` must be guarded/atomic at persistence boundary, not only validated in application service.
- LOW: Open Orders edit action predicate should match backend/spec: pending LIMIT non-conditional order only.
- LOW: service fake should key by member id and cover cross-member modification rejection.
- LOW/WATCH: use shared input component if practical to avoid modal input style drift.

## Review cycle 2 plan
1. Change repository contract to return `Optional<FuturesOrder>` from an atomic guarded update.
2. Add Spring Data modifying query with predicates for member/order/status/orderType/non-conditional fields.
3. Have service convert empty guarded update to `CoreException` and add tests for cross-member and stale/non-editable behavior.
4. Tighten frontend `isEditableLimitOrder` predicate and use shared `Input` in edit modal.
5. Re-run targeted/full validation and code-review gate.

## Review cycle 3 findings
- BLOCK: pending fill selection used stale candidate snapshots after a concurrent price edit, allowing fill at a price no longer crossed by the current move.
- COMMENT: Open Orders edit action coverage was mostly source-string based.

## Review cycle 4 findings
- BLOCK: after fixing out-of-range edits, stale candidate ordering could still violate price-priority if an already selected order changed price before its fill turn.
- BLOCK: order edits entering the current move need to be either rejected by marketability rules or picked up by refreshed fill selection.

## Review cycle 5 plan
1. Process pending limit fills one refreshed candidate at a time instead of sorting a stale batch once.
2. If the reloaded candidate price differs from the selected snapshot, defer it to the next refreshed pass so current price-priority is preserved.
3. Add regression tests for modified-out-of-range and modified-across-priority cases.
4. Re-run backend check and local review gate before PR.

## Review cycle 6 findings
- BLOCK: stale-price deferral also needed a JPA-level atomic guard; a first-level cache/stale selected entity could still make claim use an obsolete limit price.

## Review cycle 7 plan
1. Add `claimPendingLimitFill` repository contract for pending non-conditional LIMIT fills with expected limitPrice.
2. Use the guarded claim from pending limit fill processor.
3. Add persistence contract test proving stale selected limit price cannot claim after a price edit.
4. Re-run full backend/frontend validation and final local review gate.

## Review cycle 8 findings
- BLOCK: guarded claim miss could still cause the current fill loop to refresh the same transaction snapshot repeatedly.

## Review cycle 9 plan
1. Make `fillIfExecutable` return whether the current price-event fill loop should continue.
2. Stop the current loop when guarded `claimPendingLimitFill` returns empty.
3. Add regression test ensuring a guarded claim miss does not refresh candidates more than once.
4. Re-run backend/frontend validations and final local review gate.

## Review cycle 10 findings
- BLOCK: guarded claim miss continuing with later candidates can violate current priority if the transaction snapshot is stale.
- BLOCK: modified limit can become marketable after the first marketability check and before the guarded update completes.

## Review cycle 11 plan
1. On guarded claim miss, stop the current price-event fill loop and defer remaining candidates to the next fresh price event.
2. Document that stale guarded-claim miss defers remaining fills to preserve priority under concurrent edits.
3. After modify update, re-read fresh market; if the edited price became marketable, revert to original order values and reject.
4. Add tests for post-update market movement revert and guarded-claim loop stop with later candidate left pending.
5. Re-run full validation and final review gate.

## Review cycle 12 findings
- BLOCK: product spec said candidate/DB mismatch stops the current fill event, but code continued on pre-claim mismatch and tests codified same-event re-sort.

## Review cycle 13 plan
1. Return stop-loop on pre-claim candidate/current price mismatch.
2. Change regression to assert both changed and later candidates remain pending until a future fresh price event.
3. Re-run full validation and final review gate.

## CodeRabbit PR review findings (#102)
- CRITICAL/MAJOR: modify request/command/result monetary price fields used floating point at API/application boundary.
- MAJOR: edit-order button test used brittle source-string assertions instead of behavior-level submission validation.
- MINOR: post-update marketability rejection should rely on transaction rollback or handle explicit revert result.
- CRITICAL: guarded claim miss loop state was contradictory because the miss was recorded but the loop stopped immediately.
- MAJOR: product spec needed explicit transaction/lock/fill-divergence wording for pending limit modifications.

## CodeRabbit follow-up completed
1. [x] Use `BigDecimal` for modify request, command, result, and response monetary fields at the API/application boundary while keeping existing domain `double` contracts unchanged.
2. [x] Replace source-string-only edit-order test with behavior tests for valid submission, refresh/close callbacks, and invalid input blocking.
3. [x] Remove explicit revert update and rely on `@Transactional` rollback when the post-update market check rejects the edit.
4. [x] Remove unused guarded-claim-miss continuation state and keep claim miss as a stop-loop path to preserve price-priority, matching the product spec.
5. [x] Document account lock + transaction rollback and stale claim deferral semantics.

## CodeRabbit follow-up review 2 completed
- [x] MAJOR: CodeRabbit flagged `executionPrice` BigDecimal conversion as nullable-risk. Current domain uses primitive `double`, so no runtime null is possible, but direct conversion was replaced with a helper to make the primitive contract explicit and reduce review ambiguity.
