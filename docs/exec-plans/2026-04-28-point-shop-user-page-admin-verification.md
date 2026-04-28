# Point Shop, Admin, And My Page Verification

Date: 2026-04-28

## Scope

Final verification evidence for the point-shop coffee voucher flow, admin redemption handling, admin shop item management, and `/mypage` route family after PRs #36-#41 were merged.

## Verified Commands

- `cd backend && ./gradlew check --console=plain`
  - Result: pass
  - Evidence: `BUILD SUCCESSFUL`
- `cd frontend && npm run lint`
  - Result: pass
  - Evidence: `tsc --noEmit` exited 0
- `cd frontend && npm run test`
  - Result: pass
  - Evidence: 53 tests passed
- `cd frontend && npm run build`
  - Result: pass
  - Evidence: Next build generated `/shop`, `/admin/reward-redemptions`, `/admin/shop-items`, `/mypage`, `/mypage/assets`, `/mypage/points`, and `/portfolio`

## Route Smoke Checks

Local Next dev server with a local JWT cookie:

- `/shop`: 200
- `/admin/reward-redemptions`: 200
- `/admin/shop-items`: 200
- `/mypage`: 200
- `/mypage/assets`: 200
- `/mypage/points`: 200
- `/portfolio`: 307 to `/mypage`

Unauthenticated local Next dev requests:

- `/mypage`: 307 to `/login`
- `/admin/reward-redemptions`: 307 to `/login`
- `/admin/shop-items`: 307 to `/login`

Latest rerun:

- Date: 2026-04-28 KST
- Frontend command: `FUTURES_API_BASE_URL=http://127.0.0.1:8080 npm run dev -- --port 3100`
- Auth cookie route check result: `/shop`, `/admin/reward-redemptions`, `/admin/shop-items`, `/mypage`, `/mypage/assets`, and `/mypage/points` returned 200; `/portfolio` returned 307 to `/mypage`.
- Unauthenticated route check result: `/mypage`, `/admin/reward-redemptions`, and `/admin/shop-items` returned 307 to `/login`.

## Product Behavior Covered

- Coffee voucher redemption is backed by DB-managed shop items.
- Shop item sellability is derived from `active`, stock, per-member purchase count, and point balance.
- Point spending is immediate and recorded as ledger history.
- Admin can mark pending redemptions as sent or cancel/refund before send.
- Admin can list all shop items, create new items, edit mutable display/price/stock/limit/sort fields, and deactivate items through `/admin/shop-items`.
- Admin item edits preserve historical redemption snapshots, and total stock cannot be lowered below the current `sold_quantity`.
- New redemption notification is behind the reward notification boundary and currently targets `gudwns1812@naver.com` through SMTP configuration.
- `/mypage` replaces `/portfolio` as the account route family.
- `/mypage/assets` uses the supplied assets visual direction without exchange action buttons.
- Realized PnL calendar groups `netRealizedPnl` by KST day.
- `/mypage/points` shows current points and point history.

## Known Gaps

- Full browser-clicked voucher request and admin send/cancel flow was not executed against a live backend with real SMTP credentials configured. Backend service/controller tests cover the data-changing flow, and route smoke checks cover the protected pages.
- CodeRabbit was rate-limited during the admin shop item PR re-review, so Codex direct review fallback was used after the initial CodeRabbit review.
