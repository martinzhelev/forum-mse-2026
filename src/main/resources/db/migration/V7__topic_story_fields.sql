ALTER TABLE posts
    ALTER COLUMN user_id DROP DEFAULT;

ALTER TABLE replies
    ALTER COLUMN user_id DROP DEFAULT;

ALTER TABLE posts
    ALTER COLUMN title TYPE VARCHAR(500),
    ADD COLUMN modified_at TIMESTAMPTZ,
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

UPDATE posts
SET modified_at = created_at
WHERE modified_at IS NULL;

ALTER TABLE posts
    ALTER COLUMN modified_at SET NOT NULL;

ALTER TABLE replies
    ADD COLUMN modified_at TIMESTAMPTZ;

UPDATE replies
SET modified_at = created_at
WHERE modified_at IS NULL;

ALTER TABLE replies
    ALTER COLUMN modified_at SET NOT NULL;

CREATE UNIQUE INDEX uq_posts_title ON posts (title);
