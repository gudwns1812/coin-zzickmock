CREATE TABLE member_daily_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activity_date DATE NOT NULL,
    member_id BIGINT NOT NULL,
    first_seen_at TIMESTAMP(6) NOT NULL,
    last_seen_at TIMESTAMP(6) NOT NULL,
    activity_count BIGINT NOT NULL DEFAULT 1,
    first_source VARCHAR(40) NOT NULL,
    last_source VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_daily_activity PRIMARY KEY (id),
    CONSTRAINT uk_member_daily_activity_date_member UNIQUE (activity_date, member_id),
    CONSTRAINT fk_member_daily_activity_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id) ON DELETE CASCADE
);

CREATE INDEX idx_member_daily_activity_member
    ON member_daily_activity (member_id);

CREATE TABLE daily_active_user_summary (
    activity_date DATE NOT NULL,
    active_user_count BIGINT NOT NULL,
    sampled_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_daily_active_user_summary PRIMARY KEY (activity_date)
);
