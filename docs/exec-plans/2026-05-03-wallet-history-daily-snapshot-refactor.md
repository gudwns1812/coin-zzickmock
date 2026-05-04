# Wallet History Daily Snapshot Refactor Plan

## Context

`wallet_history` was implemented as a wallet mutation event log: account updates write rows with `source_type` and `source_reference`, and duplicate event references are ignored. The intended product model is different. `wallet_history` should be the account page's daily KST wallet snapshot source for:

- the recent 30-day wallet balance chart
- the daily realized PnL calendar

The product specs now define `wallet_history` as one row per member per KST `snapshotDate`, with `walletBalance` and `dailyWalletChange`. Today's value is provisional by presentation rule, not by a stored flag.

## RALPLAN-DR Summary

### Principles

- Model the product concept directly: daily snapshot, not event ledger.
- Keep wallet event/account mutation logic separate from reporting snapshots.
- Make daily rows idempotent with `(memberId, snapshotDate)`.
- Use KST as the reporting boundary everywhere the account page groups by day.
- Allow today's value to be shown as provisional by comparing `snapshotDate` with today in KST.

### Decision Drivers

- The account page needs day-level chart and calendar data, not per-fill event granularity.
- Daily wallet change must match settled wallet balance changes and exclude unrealized PnL.
- The refactor should reduce coupling between order/position mutation flows and account reporting.

### Options

1. Daily snapshot table using the existing `wallet_history` name.
   - Pros: matches current API naming and product language; minimal frontend route churn.
   - Cons: requires schema migration away from existing event-source columns.
   - Chosen because the user intent maps directly to the existing table name.

2. Add a new `wallet_daily_snapshots` table and leave `wallet_history` as event history.
   - Pros: safer data migration and clearer technical naming.
   - Cons: preserves a misleading existing concept and creates two wallet-history sources.
   - Rejected for this product because the desired domain term is `wallet_history` as daily history.

3. Keep event rows and aggregate them by day at read time.
   - Pros: small schema change.
   - Cons: event rows do not represent end-of-day wallet state, and daily PnL becomes dependent on every mutation event being perfectly recorded.
   - Rejected because it keeps the wrong domain model.

## Refactoring Plan

### 1. Schema And Domain Model

- Add a migration that converts `wallet_history` toward daily snapshots:
  - add `snapshot_date DATE NOT NULL`
  - add `daily_wallet_change DECIMAL(19, 4) NOT NULL DEFAULT 0`
  - replace event idempotency with `UNIQUE (member_id, snapshot_date)`
  - keep `recorded_at` as the actual snapshot capture timestamp
  - remove or stop using `source_type` and `source_reference`
- Existing event-style `wallet_history` rows should be truncated during the migration because they represent the wrong domain concept. Collapsing them would preserve misleading event-derived values as daily snapshots.
- New persisted daily rows always have non-null `daily_wallet_change`.
- Replace `WalletHistorySource` with a daily snapshot domain concept, for example `WalletDailySnapshot`.

### 2. Persistence Boundary

- Remove direct wallet history writes from `AccountPersistenceRepository.updateWithVersion`.
- Keep repository responsibility limited to updating `trading_accounts`.
- Add `WalletHistoryRepository` methods for idempotent baseline creation and current-day snapshot update.
- Keep read methods date-oriented, not instant/event-source-oriented.
- After successful account wallet mutations, call a reporting application service that updates the current KST day's wallet snapshot. This is not an event-history write; it updates the one daily row for that member/date.

### 3. Baseline And Current-Day Update

- Add an application service such as `SnapshotWalletHistoryService`.
- At KST day start, create each active account's baseline row idempotently. Use `UNIQUE(member_id, snapshot_date)` plus duplicate-key-safe write semantics so duplicate scheduler runs are harmless.
- On account wallet changes, update the current KST day's existing row for that member. If the row is unexpectedly missing, create it through the same idempotent baseline path before applying the update.
- Compute:
  - `walletBalance`: current settled wallet balance
  - `baselineWalletBalance`: the KST day-start wallet balance captured by the baseline row
  - `dailyWalletChange`: current row `walletBalance` minus `baselineWalletBalance`
- Baseline rows start with `dailyWalletChange = 0`.
- Add a rollover scheduler at the KST day boundary to create the new day's baseline.
- The rollover scheduler must not compute yesterday's row from the post-midnight current account balance. Doing so would mix wallet changes between midnight and the scheduler run into the wrong KST date.
- If exact historical end-of-day balances are required later, introduce a separate ledger or intraday audit trail; do not overload daily snapshots with event semantics.

### 4. Read API And Fallback

- Keep the existing wallet history endpoint if possible, but change response semantics:
  - `snapshotDate`
  - `walletBalance`
  - `dailyWalletChange`
  - `recordedAt`
- Default query returns the latest 30 KST snapshot dates.
- Today's row can be returned and displayed as provisional by date comparison.
- If no current-day row exists yet, the read API may synthesize a provisional current-day point from the current account balance and today's computed baseline, but the normal path should create the baseline row before reads need it.

### 5. Frontend Account Page

- Update `FuturesWalletHistoryPoint` to the new response shape.
- Use wallet history for both:
  - balance chart: `walletBalance`
  - calendar: `dailyWalletChange`
- Show today's `dailyWalletChange` in the calendar as provisional rather than omitting it.
- Stop deriving `/mypage/assets` calendar values from `position_history.closedAt`.
- Keep `position_history` as trade review/history detail, not as the account-page calendar source.

### 6. Tests And Verification

- Unit-test KST date boundary handling.
- Persistence-test idempotent baseline creation/no-duplicate behavior for `(memberId, snapshotDate)`, including duplicate-key-safe writes.
- Migration-test or schema-test that `UNIQUE(member_id, snapshot_date)` exists.
- Service-test that account wallet mutations update only the current KST daily row, not per-event rows.
- Service-test snapshot generation from account balances and the current KST day's baseline wallet balance.
- Service-test that rollover baseline creation does not recalculate yesterday from a post-midnight current balance.
- API-test provisional current-day shape and default 30-day window.
- Frontend-test chart sorting by `snapshotDate` and calendar grouping from `dailyWalletChange`.

## ADR

### Decision

Use `wallet_history` as a KST daily wallet snapshot table.

### Drivers

- The account page needs day-level wallet balance and realized PnL.
- Mutation events are not the same concept as daily reporting history.
- A unique daily row gives simple, stable chart and calendar semantics.

### Alternatives Considered

- Keep event rows and aggregate by day.
- Add a new `wallet_daily_snapshots` table.

### Why Chosen

The existing product language and API already call this `wallet_history`, and the user's intent is explicitly daily wallet history. Refactoring the table semantics removes misleading event-source language and makes the account page's data source coherent.

### Consequences

- Existing event-source code and tests need deletion or rewrite into daily baseline creation and current-day update behavior.
- Existing event-style `wallet_history` rows will be truncated because they are not valid daily snapshots.
- If future audit/event requirements appear, they should use a separate ledger table.

### Follow-ups

- Regenerate schema docs after migration code lands.

## Execution Guidance

- Sequential `ralph` path: one executor can update schema/domain/repository/service, then frontend, then docs/tests.
- Team path:
  - Backend executor: schema/domain/repository/scheduler/API.
  - Frontend executor: response type, chart, calendar usage.
  - Test engineer: KST boundary, idempotency, provisional current-day API behavior, frontend utilities.
  - Writer: regenerate schema docs and reconcile product docs after implementation.

## Team Verification Path

- Backend: focused account persistence/service/API tests, then broader backend test slice.
- Frontend: wallet history chart/calendar utility tests and `/mypage/assets` smoke check.
- Documentation: confirm product specs, generated schema docs, and implementation behavior agree.
