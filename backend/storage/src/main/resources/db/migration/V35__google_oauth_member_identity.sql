ALTER TABLE member_credentials
    MODIFY COLUMN account VARCHAR(64) NULL;

ALTER TABLE member_credentials
    MODIFY COLUMN password_hash VARCHAR(255) NULL;

CREATE TABLE member_oauth_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NULL,
    provider_name VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_member_oauth_identities PRIMARY KEY (id),
    CONSTRAINT uk_member_oauth_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_member_oauth_identity_member_provider UNIQUE (member_id, provider),
    CONSTRAINT fk_member_oauth_identity_member FOREIGN KEY (member_id) REFERENCES member_credentials(id) ON DELETE CASCADE
);

CREATE INDEX idx_member_oauth_identities_member_id
    ON member_oauth_identities (member_id);

CREATE TABLE member_oauth_pending_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NULL,
    provider_name VARCHAR(255) NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_failed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_member_oauth_pending_links PRIMARY KEY (id),
    CONSTRAINT uk_member_oauth_pending_links_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_member_oauth_pending_provider_subject
    ON member_oauth_pending_links (provider, provider_subject);
CREATE INDEX idx_member_oauth_pending_expires_at
    ON member_oauth_pending_links (expires_at);
