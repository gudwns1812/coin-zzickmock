# 상점 즉시 구매 이력 원장 도입

이 계획서는 `docs/exec-plans/README.md`와 `docs/exec-plans/plan-template.md`의 구조를 따른다.

## 목적 / 큰 그림

현재 포지션 엿보기권과 리필권은 구매 성공 시 포인트 차감과 효과 적용은 되지만, `/mypage/redemptions`의 "구매/교환 내역"에는 표시되지 않는다.
원인은 즉시 구매 상품이 `reward_redemption_requests`를 만들지 않고, 화면은 해당 테이블만 읽기 때문이다.

이 작업은 즉시 구매 상품을 위한 성공 구매 원장 `reward_shop_purchases`를 추가하고, 교환권 신청과 즉시 구매를 합친 상점 내역 API/UI를 만든다.
완료 후 사용자는 엿보기권/리필권 구매와 커피 교환권 신청을 한 화면에서 구분해서 볼 수 있고, 포인트 내역에서는 내부 UUID가 노출되지 않는다.

## 진행 현황

- [x] 계획 초안 작성
- [x] 사용자 승인
- [x] 구현
- [x] 테스트
- [x] review 스킬 기반 검토 확인
- [x] 작업 종료 처리(완료 판단 및 completed 이동)

## 놀라움과 발견

- `POSITION_PEEK` 구매는 `reward_item_balances.remaining_quantity`를 증가시키고 `reward_point_histories`에 `INSTANT_SHOP_PURCHASE`를 남기지만, 별도 item snapshot이 없다.
- 현재 `RewardPointHistory.instantShopPurchaseDeduct`의 `sourceReference`에는 랜덤 UUID가 들어가며, 프론트가 이를 그대로 보여준다.
- 제품 문서는 이미 `ShopPurchase`와 "상점 구매" 이력을 언급하지만, 현재 DB schema에는 전용 구매 원장이 없다.

## 의사결정 기록

- 즉시 구매 상품은 `reward_redemption_requests`에 넣지 않는다. 교환권 신청 lifecycle과 즉시 구매 성공 event의 의미가 다르기 때문이다.
- `reward_shop_purchases`는 status 없는 성공-only 불변 원장으로 만든다. row 존재 자체가 구매 효과가 적용됐다는 의미다.
- 커피 교환권 신청은 이번 PR에서 구매 원장에 중복 기록하지 않는다. 통합 read model에서 `reward_redemption_requests`와 즉시 구매 원장을 합쳐 보여준다.
- 새 구매부터는 `purchaseId`를 생성해 `reward_shop_purchases.purchase_id`와 `reward_point_histories.source_reference`의 공통 correlation key로 사용한다.
- 과거 `INSTANT_SHOP_PURCHASE` 포인트 이력은 item snapshot이 없어 백필하지 않는다. 대신 포인트 내역 UI에서 UUID를 숨기고 "상점 즉시 구매"로 표시한다.

## 결과 및 회고

- `reward_shop_purchases` 성공 구매 원장과 통합 상점 history API를 추가했다.
- 엿보기권/리필권 즉시 구매는 같은 `purchaseId`를 구매 원장과 포인트 원장에 함께 남긴다.
- `/mypage/redemptions`는 즉시 구매와 교환권 신청을 source-aware row로 표시하고, `/mypage/points`는 `INSTANT_SHOP_PURCHASE`의 raw UUID를 숨긴다.
- 동시각 통합 내역은 `eventAt desc`, kind priority desc, 같은 종류 내부 id desc로 안정 정렬한다. true commit chronology는 서로 다른 테이블 id만으로 복원하지 않는다.
- 로컬 기본 MySQL DB에는 다른 작업의 `V31__add_community_posts.sql` 적용 이력이 남아 있었다. 이번 PR의 구매 원장 migration은 `V32`로 올려 version 충돌을 피했고, 런타임 스모크는 임시 MySQL DB에서 새로 Flyway를 적용해 수행했다.

## 범위

- 이번에 하는 것(in scope):
  - `reward_shop_purchases` Flyway migration, JPA entity, persistence repository 추가
  - core domain/application result/repository/service 추가
  - `PurchaseShopItemService`에서 즉시 구매 성공 원장 기록
  - `GET /api/futures/shop/history` 통합 내역 API 추가
  - `/mypage/redemptions`를 통합 구매/교환 내역 화면으로 갱신
  - `/mypage/points`에서 `INSTANT_SHOP_PURCHASE` UUID 미노출 처리
  - 제품 명세와 generated schema 갱신
  - backend/frontend 테스트와 런타임 검증
- 이번에 하지 않는 것(out of scope):
  - 과거 즉시 구매 이력 백필
  - 즉시 구매 취소/환불 lifecycle
  - 커피 교환권 신청을 `reward_shop_purchases`에 중복 기록
- 후속 작업(선택):
  - 관리자용 상점 구매 원장 조회
  - 다중 수량 구매 지원
  - 즉시 구매 환불 정책이 생길 경우 별도 상태/보정 원장 추가

## 요구 사항 요약

- 기능 요구 사항:
  - 엿보기권/리필권 구매 성공 시 구매 원장 row가 남아야 한다.
  - 구매 원장 row에는 구매 시점의 item code/name/type/price/point snapshot이 남아야 한다.
  - `/api/futures/shop/history`는 즉시 구매와 교환권 신청을 함께 반환해야 한다.
  - `/mypage/redemptions`는 즉시 구매 row와 교환 신청 row를 source-aware로 렌더링해야 한다.
  - pending 교환 신청만 취소할 수 있어야 한다.
  - 포인트 내역은 `INSTANT_SHOP_PURCHASE`의 raw UUID를 사용자에게 보여주지 않아야 한다.
- 비기능 요구 사항:
  - DB 변경은 Flyway migration과 `docs/generated/db-schema.md`를 함께 갱신한다.
  - web은 application service만 호출하고 storage에 직접 의존하지 않는다.
  - 통합 내역 정렬은 public UUID가 아니라 `eventAt desc`, kind priority desc, 같은 종류 내부 `sortSequence desc` 기준으로 안정적이어야 한다.

## 맥락과 길잡이

- 관련 문서:
  - `AGENTS.md`
  - `BACKEND.md`
  - `FRONTEND.md`
  - `backend/AGENTS.md`
  - `backend/core/AGENTS.md`
  - `backend/app/AGENTS.md`
  - `backend/storage/AGENTS.md`
  - `backend/docs/architecture/foundations.md`
  - `backend/core/docs/application-and-providers.md`
  - `backend/storage/docs/persistence-rules.md`
  - `backend/storage/docs/schema-and-migration.md`
  - `docs/product-specs/coin-futures-platform-mvp.md`
  - `docs/product-specs/coin-futures-simulation-rules.md`
  - `docs/product-specs/coin-futures-screen-spec.md`
  - `docs/generated/db-schema.md`
  - `frontend/README.md`
  - `docs/design-docs/ui-design/README.md`
- 관련 코드 경로:
  - `backend/core/src/main/java/coin/coinzzickmock/feature/reward/domain`
  - `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application`
  - `backend/storage/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence`
  - `backend/storage/src/main/resources/db/migration`
  - `backend/app/src/main/java/coin/coinzzickmock/feature/reward/web`
  - `frontend/lib/futures-api.ts`
  - `frontend/lib/futures-client-api.ts`
  - `frontend/components/rewards/RewardRedemptionHistoryClient.tsx`
  - `frontend/app/(main)/mypage/redemptions/page.tsx`
  - `frontend/app/(main)/mypage/points/page.tsx`
- 선행 조건:
  - 기존 worktree 변경을 되돌리지 않는다.
  - 스키마 변경 전에 현재 migration 번호와 `docs/generated/db-schema.md`의 reward 섹션을 확인한다.

## 문서 원문 대조표

| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| product spec | `docs/product-specs/coin-futures-platform-mvp.md`, `docs/product-specs/coin-futures-simulation-rules.md`, `docs/product-specs/coin-futures-screen-spec.md` | 즉시 구매는 redemption request가 아니며, 상점 구매 이력은 별도 기록 대상이다 | 즉시 구매 원장과 통합 history read model 추가 | 엿보기권을 억지로 교환권 신청으로 저장 | 제품 명세 diff와 controller integration |
| backend application | `backend/docs/architecture/foundations.md`, `backend/core/docs/application-and-providers.md` | web -> application, application -> domain/repository contract, web -> storage 금지 | `GetRewardShopHistoryService`와 core-owned result/contract 추가 | controller에서 storage query 직접 호출 | `architectureLint`, service/controller tests |
| backend persistence / DB | `backend/storage/docs/persistence-rules.md`, `backend/storage/docs/schema-and-migration.md`, `docs/generated/db-schema.md` | schema 변경은 migration + generated schema + entity/repository 정렬 | `reward_shop_purchases` migration/entity/repository 추가 | point history를 구매 원장처럼 재사용 | `:storage:test`, `:app:verifyBootJarStorageMigrations` |
| frontend UX/API | `FRONTEND.md`, `frontend/README.md`, `docs/design-docs/ui-design/README.md` | Server Component 우선, API 함수 경계 재사용, UI 변경은 runtime 검증 | `getRewardShopHistory()`와 source-aware 렌더링 | raw `sourceReference`를 사용자에게 그대로 표시 | frontend tests, lint/build, browser smoke |

대조표 점검:

- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품/DB 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 어느 쪽을 우선할지 명시했다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인할 수 있다.

## 작업 계획

1. 제품/스키마 문서에 반영할 계약을 먼저 고정한다.
   - 즉시 구매 성공 row는 status 없는 terminal event다.
   - `purchase_count`와 `sold_quantity`는 성공한 즉시 구매와 active redemption request를 포함한다.
   - `REJECTED`/`CANCELLED` redemption만 카운트와 재고를 복구한다.
2. DB migration을 추가한다.
   - `reward_shop_purchases`
   - unique `purchase_id`
   - member/shop item FK
   - check constraints: positive price/point, `quantity = 1`, constrained `effect_type`
   - `(member_id, purchased_at, id)` index
3. backend core를 추가한다.
   - `RewardShopPurchase`
   - `RewardShopPurchaseRepository`
   - shop history projection/result
   - `GetRewardShopHistoryService`
4. storage adapter를 구현한다.
   - purchase entity/repository
   - member history query projection with `eventAt` and non-public `sortSequence`
   - redemption history projection도 kind priority와 같은 테이블 internal id 기반 sort key를 제공
5. `PurchaseShopItemService`를 수정한다.
   - `purchaseId`를 한 번 생성
   - purchase ledger 저장
   - point history `sourceReference`에 동일 `purchaseId` 사용
6. app web adapter를 추가한다.
   - `GET /api/futures/shop/history`
   - `RewardShopHistoryResponse`
7. frontend를 수정한다.
   - `futures-api.ts`에 `ShopHistoryRow`와 `getRewardShopHistory`
   - `/mypage/redemptions`와 `RewardRedemptionHistoryClient`를 source-aware로 변경
   - `/mypage/points`의 `INSTANT_SHOP_PURCHASE` label/metadata 숨김 처리
8. docs와 tests를 갱신한다.
   - product spec, screen spec, db schema
   - backend service/storage/controller tests
   - frontend render/source tests
9. 검증 명령과 브라우저 smoke를 실행하고 계획 문서를 갱신한다.

## 구체적인 단계

1. `docs/product-specs/coin-futures-simulation-rules.md`와 `coin-futures-screen-spec.md`에 최종 UX/data contract를 먼저 반영한다.
2. 다음 migration 번호로 `reward_shop_purchases` 생성 migration을 작성한다.
3. `docs/generated/db-schema.md`에 새 테이블과 reward 기존 테이블 의미 변경을 반영한다.
4. core domain/result/repository/service를 추가한다.
5. storage entity/repository/query를 추가한다.
6. `PurchaseShopItemService`에 purchase ledger 기록을 연결한다.
7. `RewardController`에 `/shop/history` endpoint를 추가한다.
8. frontend API와 화면 컴포넌트를 변경한다.
9. 테스트를 추가하고 검증 명령을 실행한다.
10. 계획 문서의 진행 현황, 놀라움과 발견, 결과 및 회고를 최신화한다.

## 수용 기준(테스트 가능한 형태)

- 엿보기권 구매 후 `reward_shop_purchases`에 `position.peek` snapshot row가 생긴다.
- 같은 구매의 `purchase_id`와 `reward_point_histories.source_reference`가 일치한다.
- `/api/futures/shop/history`는 즉시 구매 row를 `kind = INSTANT_PURCHASE`로 반환한다.
- `/api/futures/shop/history`는 교환 신청 row를 `kind = REDEMPTION_REQUEST`로 반환한다.
- 통합 history는 `eventAt desc`, kind priority desc, 같은 종류 내부 `sortSequence desc`로 정렬된다.
- `/mypage/redemptions`에서 즉시 구매 row는 연락처/취소 버튼 없이 표시된다.
- pending 교환 신청 row는 기존처럼 취소 가능하다.
- `/mypage/points`에서 `INSTANT_SHOP_PURCHASE` raw UUID가 보이지 않는다.
- 과거 즉시 구매 이력은 백필하지 않는다는 컷오프가 문서에 명시된다.

## 위험과 완화

- 위험 시나리오 1: 구매 원장과 포인트 원장이 다른 purchase id를 갖는다.
  - 예방: service에서 `purchaseId`를 한 번만 생성해 두 저장 호출에 전달한다.
  - 완화: service test로 동일성 검증.
  - 복구: 잘못 저장된 데이터는 별도 수동 migration 또는 운영 보정이 필요하므로 PR 전 테스트로 차단한다.
- 위험 시나리오 2: 통합 내역 정렬이 UUID 문자열 정렬로 흔들린다.
  - 예방: public id와 별개로 `eventAt`, kind priority, 같은 종류 내부 `sortSequence`를 projection에 둔다.
  - 완화: 같은 timestamp rows를 포함한 정렬 테스트 추가.
  - 복구: read model 정렬 로직 수정으로 복구 가능.
- 위험 시나리오 3: 기존 포인트 내역의 legacy UUID가 계속 노출된다.
  - 예방: frontend label/metadata mapping을 source-aware로 변경한다.
  - 완화: frontend test로 `INSTANT_SHOP_PURCHASE` UUID 미노출 검증.
  - 복구: 화면 렌더링만 수정하면 되므로 데이터 변경 없이 복구 가능.

## 검증 절차

- 실행 명령:
  - `cd backend && ./gradlew :app:test --tests "coin.coinzzickmock.feature.reward.application.service.PurchaseShopItemServiceTest" --tests "coin.coinzzickmock.feature.reward.application.service.RewardRedemptionServiceTest" --tests "coin.coinzzickmock.feature.reward.web.RewardControllerIntegrationTest" --tests "coin.coinzzickmock.feature.reward.web.RewardControllerAuthorizationTest" --console=plain`
  - `cd backend && ./gradlew :storage:test --console=plain`
  - `cd backend && ./gradlew :app:verifyBootJarStorageMigrations --console=plain`
  - `cd backend && ./gradlew architectureLint --console=plain`
  - `cd backend && ./gradlew check --console=plain`
  - `npm test --workspace frontend`
  - `npm run lint`
  - `npm run build`
- 런타임 확인:
  - frontend dev server를 실행한다.
  - `/mypage/redemptions`에서 즉시 구매와 교환 신청 row 표시를 확인한다.
  - `/mypage/points`에서 `INSTANT_SHOP_PURCHASE` raw UUID가 숨겨졌는지 확인한다.
- 기대 결과:
  - 모든 targeted/full 검증이 통과한다.
  - 브라우저 콘솔과 네트워크에 새 오류가 없다.
- 실패 시 확인할 것:
  - migration 번호와 H2/MySQL 호환 SQL
  - reward table FK 컬럼 타입
  - application read model 정렬 key
  - frontend nullable field 렌더링

## 반복 실행 가능성 및 복구

- 반복 실행 시 안전성:
  - migration은 한 번만 적용된다.
  - purchase write는 신규 구매 transaction에서만 발생한다.
- 위험한 단계:
  - DB migration
  - 구매 transaction에 새 저장 호출 추가
- 롤백 또는 재시도 방법:
  - 구현 전이면 migration 파일 삭제와 schema 문서 되돌림.
  - PR 후라면 새 보정 migration으로 처리하고 기존 migration은 수정하지 않는다.

## 산출물과 메모

- 관련 로그:
  - Ralplan context snapshot: `.omx/context/shop-purchase-history-ledger-20260514T025009Z.md`
  - Architect review: compose/env typo는 사용자 로컬 변경으로 PR scope에서 제외, coffee voucher 100P 명세 정정, same timestamp ordering 회귀 테스트 추가
  - AI slop cleaner: 새 fallback-like/slop finding 없음, 기존 fallback 문구는 기존 API/문서 경계로 분류
- 검증 결과:
  - backend targeted reward tests: PASS
  - `:storage:test`: PASS
  - `:app:verifyBootJarStorageMigrations`: PASS
  - `architectureLint`: PASS
  - `./gradlew check`: PASS
  - frontend tests: PASS, 189 passed
  - `npm run lint`: PASS
  - `npm run build`: PASS
  - browser smoke: PASS, 임시 MySQL DB + `FUTURES_API_BASE_URL=http://127.0.0.1:8080` dev server에서 `/mypage/redemptions`, `/mypage/points` 200 렌더링 및 console error 없음
