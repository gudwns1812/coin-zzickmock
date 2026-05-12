ALTER TABLE member_credentials
    ADD COLUMN withdrawn_at TIMESTAMP(6) NULL;

CREATE INDEX idx_member_credentials_withdrawn_at
    ON member_credentials (withdrawn_at);
