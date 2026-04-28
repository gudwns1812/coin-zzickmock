# Point Shop, Admin, And My Page Verification

Date: 2026-04-28

## Scope

Final verification evidence for the point-shop coffee voucher flow, admin redemption handling, and `/mypage` route family after PRs 1-4 were merged.

## Verified Commands

- `cd backend && ./gradlew check --console=plain`
  - Result: pass
  - Evidence: `BUILD SUCCESSFUL`
- `cd frontend && npm run lint`
  - Result: pass
  - Evidence: `tsc --noEmit` exited 0
- `cd frontend && npm run test`
  - Result: pass
  - Evidence: 50 tests passed
- `cd frontend && npm run build`
  - Result: pass
  - Evidence: Next build generated `/shop`, `/admin/reward-redemptions`, `/mypage`, `/mypage/assets`, `/mypage/points`, and `/portfolio`

## Route Smoke Checks

Local Next dev server with a local JWT cookie:

- `/shop`: 200
- `/admin/reward-redemptions`: 200
- `/mypage`: 200
- `/mypage/assets`: 200
- `/mypage/points`: 200
- `/portfolio`: 307 to `/mypage`

Unauthenticated local Next dev requests:

- `/mypage`: 307 to `/login`
- `/admin/reward-redemptions`: 307 to `/login`

## Product Behavior Covered

- Coffee voucher redemption is backed by DB-managed shop items.
- Shop item sellability is derived from `active`, stock, per-member purchase count, and point balance.
- Point spending is immediate and recorded as ledger history.
- Admin can mark pending redemptions as sent or cancel/refund before send.
- New redemption notification is behind the reward notification boundary and currently targets `gudwns1812@naver.com` through SMTP configuration.
- `/mypage` replaces `/portfolio` as the account route family.
- `/mypage/assets` uses the supplied assets visual direction without exchange action buttons.
- Realized PnL calendar groups `netRealizedPnl` by KST day.
- `/mypage/points` shows current points and point history.

## Known Gaps

- Full browser-clicked voucher request and admin send/cancel flow was not executed against a live backend with SMTP configured in this PR.
- CodeRabbit was rate-limited during recent PRs, so Codex direct review fallback was used where quota was unavailable.
