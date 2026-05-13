CREATE TABLE community_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_member_id BIGINT NOT NULL,
    author_nickname VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_json LONGTEXT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_community_posts PRIMARY KEY (id),
    CONSTRAINT fk_community_posts_author
        FOREIGN KEY (author_member_id) REFERENCES member_credentials (id),
    CONSTRAINT chk_community_posts_category
        CHECK (category IN ('NOTICE', 'CHART_ANALYSIS', 'COIN_INFORMATION', 'CHAT')),
    CONSTRAINT chk_community_posts_view_count CHECK (view_count >= 0),
    CONSTRAINT chk_community_posts_like_count CHECK (like_count >= 0),
    CONSTRAINT chk_community_posts_comment_count CHECK (comment_count >= 0)
);

CREATE INDEX idx_community_posts_category_deleted_created
    ON community_posts (category, deleted_at, created_at);

CREATE INDEX idx_community_posts_deleted_created
    ON community_posts (deleted_at, created_at);

CREATE INDEX idx_community_posts_author_created
    ON community_posts (author_member_id, created_at);

CREATE TABLE community_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_member_id BIGINT NOT NULL,
    author_nickname VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_community_comments PRIMARY KEY (id),
    CONSTRAINT fk_community_comments_post
        FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_comments_author
        FOREIGN KEY (author_member_id) REFERENCES member_credentials (id)
);

CREATE INDEX idx_community_comments_post_deleted_created
    ON community_comments (post_id, deleted_at, created_at);

CREATE INDEX idx_community_comments_author_created
    ON community_comments (author_member_id, created_at);

CREATE TABLE community_post_likes (
    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_community_post_likes PRIMARY KEY (post_id, member_id),
    CONSTRAINT fk_community_post_likes_post
        FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_post_likes_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id)
);

CREATE INDEX idx_community_post_likes_member_created
    ON community_post_likes (member_id, created_at);

CREATE TABLE community_post_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NULL,
    uploader_member_id BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    public_url VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_community_post_images PRIMARY KEY (id),
    CONSTRAINT uk_community_post_images_object_key UNIQUE (object_key),
    CONSTRAINT fk_community_post_images_post
        FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_post_images_uploader
        FOREIGN KEY (uploader_member_id) REFERENCES member_credentials (id),
    CONSTRAINT chk_community_post_images_status
        CHECK (status IN ('PRESIGNED', 'ATTACHED', 'ORPHANED')),
    CONSTRAINT chk_community_post_images_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_community_post_images_post_status
    ON community_post_images (post_id, status);

CREATE INDEX idx_community_post_images_uploader_status_created
    ON community_post_images (uploader_member_id, status, created_at);
