-- Forward-only canonicalization:
-- app rollback after this migration requires the compensating SQL documented in
-- docs/release-docs/reward-redemption-status-migration-runbook.md.
UPDATE reward_redemption_requests
   SET status = 'APPROVED'
 WHERE status = 'SENT';

UPDATE reward_redemption_requests
   SET status = 'REJECTED'
 WHERE status = 'CANCELLED_REFUNDED';

ALTER TABLE reward_redemption_requests
    ADD CONSTRAINT chk_reward_redemption_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    );

CREATE UNIQUE INDEX uk_reward_point_histories_source_once
    ON reward_point_histories (source_type, source_reference, history_type);
