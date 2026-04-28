# Reward Redemption Status Migration Runbook

## Scope

`V14__rename_reward_redemption_statuses.sql` is a forward-only data migration for reward redemption states.

It maps:

- `SENT` -> `APPROVED`
- `CANCELLED_REFUNDED` -> `REJECTED`

The application keeps runtime compatibility for legacy admin filters and endpoints, but the database values after V14 are the new canonical states.

## Rollback Rule

Do not roll back application code alone after V14 has run. Older code cannot hydrate `APPROVED`, `REJECTED`, or `CANCELLED` through the old enum contract.

If an application rollback is unavoidable, apply a compensating database migration first:

```sql
ALTER TABLE reward_redemption_requests
    DROP CHECK chk_reward_redemption_requests_status;

UPDATE reward_redemption_requests
   SET status = 'SENT'
 WHERE status = 'APPROVED';

UPDATE reward_redemption_requests
   SET status = 'CANCELLED_REFUNDED'
 WHERE status IN ('REJECTED', 'CANCELLED');
```

`CANCELLED` has no exact legacy equivalent, so rollback maps it to `CANCELLED_REFUNDED` with the same accounting outcome: points, stock, and member purchase count have already been restored once.

## Preflight

Before deploying V14 to a shared database, check that the refund idempotency index will not fail:

```sql
SELECT source_type, source_reference, history_type, COUNT(*) AS duplicate_count
  FROM reward_point_histories
 WHERE source_type IS NOT NULL
   AND source_reference IS NOT NULL
 GROUP BY source_type, source_reference, history_type
HAVING COUNT(*) > 1;
```

Resolve duplicates before applying the migration.
