CREATE TABLE member_daily_activity_v23 (
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
    CONSTRAINT pk_member_daily_activity_v23 PRIMARY KEY (id)
);

INSERT INTO member_daily_activity_v23 (
    activity_date,
    member_id,
    first_seen_at,
    last_seen_at,
    activity_count,
    first_source,
    last_source,
    created_at,
    updated_at
)
SELECT activity_date,
       member_id,
       first_seen_at,
       last_seen_at,
       activity_count,
       first_source,
       last_source,
       created_at,
       updated_at
  FROM member_daily_activity
 ORDER BY activity_date, member_id;

DROP TABLE member_daily_activity;

ALTER TABLE member_daily_activity_v23 RENAME TO member_daily_activity;

ALTER TABLE member_daily_activity
    ADD CONSTRAINT uk_member_daily_activity_date_member UNIQUE (activity_date, member_id);

CREATE INDEX idx_member_daily_activity_member
    ON member_daily_activity (member_id);

ALTER TABLE member_daily_activity
    ADD CONSTRAINT fk_member_daily_activity_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id) ON DELETE CASCADE;
