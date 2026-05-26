CREATE TABLE user_analytics_preference (
    user_analytics_preference_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_overridden BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_analytics_preference_user UNIQUE (user_id),
    CONSTRAINT fk_user_analytics_preference_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

INSERT INTO user_analytics_preference (user_id, enabled, updated_at, user_overridden)
SELECT user_id, FALSE, CURRENT_TIMESTAMP, FALSE FROM user;
