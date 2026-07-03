-- Existing database migration: add one-like-per-user records for forum posts.
CREATE TABLE IF NOT EXISTS forum_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_forum_like_post_user (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES forum_post(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT '论坛点赞记录表';
