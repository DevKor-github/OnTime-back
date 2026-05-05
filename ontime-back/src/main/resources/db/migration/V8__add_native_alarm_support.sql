CREATE TABLE user_alarm_setting (
    user_alarm_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    alarms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_alarm_offset_minutes INT NOT NULL DEFAULT 5,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_alarm_setting_user UNIQUE (user_id),
    CONSTRAINT fk_user_alarm_setting_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

INSERT INTO user_alarm_setting (user_id, alarms_enabled, default_alarm_offset_minutes, updated_at)
SELECT user_id, TRUE, 5, CURRENT_TIMESTAMP FROM user;

CREATE TABLE user_device (
    user_device_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    app_version VARCHAR(128),
    os_version VARCHAR(128),
    supports_native_alarm BOOLEAN NOT NULL DEFAULT FALSE,
    native_alarm_provider VARCHAR(40) NOT NULL,
    fallback_provider VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    firebase_token TEXT,
    session_access_token TEXT,
    session_refresh_token TEXT,
    CONSTRAINT uk_user_device_user_device UNIQUE (user_id, device_id),
    CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_device_user_active ON user_device(user_id, active);

CREATE TABLE user_alarm_status (
    user_alarm_status_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_device_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    reconciled_at TIMESTAMP NOT NULL,
    schedule_window_start TIMESTAMP,
    schedule_window_end TIMESTAMP,
    alarm_coverage_start TIMESTAMP,
    alarm_coverage_end TIMESTAMP,
    status VARCHAR(40) NOT NULL,
    permission_issue VARCHAR(60),
    native_alarm_provider VARCHAR(40) NOT NULL,
    fallback_provider VARCHAR(40) NOT NULL,
    armed_schedule_count INT NOT NULL DEFAULT 0,
    armed_schedule_ids TEXT,
    skipped_schedule_count INT NOT NULL DEFAULT 0,
    failures TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_alarm_status_device UNIQUE (user_device_id),
    CONSTRAINT fk_user_alarm_status_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_alarm_status_device FOREIGN KEY (user_device_id) REFERENCES user_device (user_device_id) ON DELETE CASCADE
);
