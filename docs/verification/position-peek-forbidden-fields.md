# Position Peek Forbidden-Field Verification

## Scope

This checklist gates the item-gated ranked user position peek feature. It covers public API contracts, frontend UI surfaces, and documentation for the viewer-owned snapshot model.

## Required public fields

Position peek snapshot responses and UI may expose only:

- target display context: nickname/rank context captured at snapshot time
- snapshot metadata: `peekId`, `createdAt`
- public position rows: `symbol`, `positionSide`, `leverage`, `positionSize`, `entryPrice`, `notionalValue`, `unrealizedPnl`, `roi`
- item state: `remainingPeekItemCount`

## Forbidden fields

The following must not appear in position peek public responses, frontend route state, or UI copy:

- raw ids: `memberId`, `targetMemberId`, `viewerMemberId`, account ids, order ids, position history ids
- TP/SL/self-management fields: `takeProfitPrice`, `stopLossPrice`, `pendingCloseQuantity`, `closeableQuantity`
- mutation/action affordances: close, edit, cancel, order management, history drilldown
- live-refresh affordances that imply free re-read of the target position

## Contract review checklist

- Leaderboard list/search returns `targetToken` but no raw member id.
- `targetToken` is passed by request body or server-side lookup key, not long-lived query string.
- `POST /api/futures/position-peeks` consumes exactly one `POSITION_PEEK` item and creates one immutable snapshot in the same transaction.
- Failed token validation, deleted/banned target, or projection/persistence failure does not decrement item balance.
- `GET /api/futures/position-peeks/{peekId}` checks viewer ownership and reads stored snapshot rows only.
- Existing snapshots remain readable after rank drift or leaderboard removal.
- No-open-position targets create a successful empty snapshot.
- Existing self-position endpoints and TP/SL management UI remain unchanged.

## Verification commands

```bash
node scripts/check-position-peek-forbidden-fields.mjs
rtk git diff --check
```

Backend/frontend feature owners should additionally run the targeted tests named in `.omx/plans/ralplan-user-position-peek.md` once their implementation slices exist.
