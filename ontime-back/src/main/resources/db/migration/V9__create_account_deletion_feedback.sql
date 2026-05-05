CREATE TABLE account_deletion_feedback (
    feedback_id BINARY(16) PRIMARY KEY,
    deleted_user_id BIGINT,
    social_type VARCHAR(255),
    email_hash VARCHAR(64),
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
