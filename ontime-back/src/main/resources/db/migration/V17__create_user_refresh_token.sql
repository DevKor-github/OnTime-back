CREATE TABLE user_refresh_token (
    user_refresh_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_refresh_token_token UNIQUE (refresh_token),
    CONSTRAINT fk_user_refresh_token_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_refresh_token_user ON user_refresh_token(user_id);

INSERT INTO user_refresh_token (user_id, refresh_token, created_at, updated_at)
SELECT user_id, refresh_token, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user
WHERE refresh_token IS NOT NULL AND refresh_token <> '';
