-- 1. API_LOG 테이블 변경
ALTER TABLE api_log MODIFY COLUMN request_method VARCHAR(255);
ALTER TABLE api_log MODIFY COLUMN client_ip VARCHAR(255);

-- 2. USER 테이블 변경
ALTER TABLE user MODIFY COLUMN email VARCHAR(255);
ALTER TABLE user MODIFY COLUMN `role` VARCHAR(255);
ALTER TABLE user MODIFY COLUMN social_type VARCHAR(255);
ALTER TABLE user MODIFY COLUMN social_id VARCHAR(255);
ALTER TABLE user MODIFY COLUMN refresh_token VARCHAR(255);
ALTER TABLE user MODIFY COLUMN firebase_token VARCHAR(255);
ALTER TABLE user MODIFY COLUMN social_login_token VARCHAR(255);


ALTER TABLE schedule DROP FOREIGN KEY fk_schedule_place;
ALTER TABLE preparation_schedule DROP FOREIGN KEY fk_preparation_schedule;
ALTER TABLE preparation_schedule DROP FOREIGN KEY fk_next_preparation;
ALTER TABLE preparation_user DROP FOREIGN KEY fk_preparation_user;
ALTER TABLE preparation_user DROP FOREIGN KEY fk_next_preparation_user;

-- 3. PLACE 테이블 변경
ALTER TABLE place MODIFY COLUMN place_id BINARY(16);
ALTER TABLE place MODIFY COLUMN place_name VARCHAR(255);

-- 4. SCHEDULE 테이블 변경
ALTER TABLE schedule MODIFY COLUMN schedule_id BINARY(16);
ALTER TABLE schedule MODIFY COLUMN place_id BINARY(16);
ALTER TABLE schedule MODIFY COLUMN schedule_name VARCHAR(30);

-- 5. FEEDBACK 테이블 변경
ALTER TABLE feedback MODIFY COLUMN feedback_id BINARY(16);
ALTER TABLE feedback MODIFY COLUMN message VARCHAR(255);
ALTER TABLE feedback MODIFY COLUMN create_at TIMESTAMP;

-- 6. FRIEND_SHIP 테이블 변경
ALTER TABLE friend_ship MODIFY COLUMN friend_ship_id BINARY(16);
ALTER TABLE friend_ship MODIFY COLUMN accept_status VARCHAR(255);

-- 7. PREPARATION_SCHEDULE 테이블 변경
ALTER TABLE preparation_schedule MODIFY COLUMN preparation_schedule_id BINARY(16);
ALTER TABLE preparation_schedule MODIFY COLUMN schedule_id BINARY(16);
ALTER TABLE preparation_schedule MODIFY COLUMN next_preparation_id BINARY(16);

-- 8. PREPARATION_USER 테이블 변경
ALTER TABLE preparation_user MODIFY COLUMN preparation_id BINARY(16);
ALTER TABLE preparation_user MODIFY COLUMN next_preparation_id BINARY(16);

-- 9. USER_SETTING 테이블 변경
ALTER TABLE user_setting MODIFY COLUMN user_setting_id BINARY(16);

ALTER TABLE schedule
    ADD CONSTRAINT fk_schedule_place FOREIGN KEY (place_id) REFERENCES place(place_id) ON DELETE SET NULL;


ALTER TABLE preparation_schedule
    ADD CONSTRAINT fk_preparation_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE;

ALTER TABLE preparation_schedule
    ADD CONSTRAINT fk_next_preparation FOREIGN KEY (next_preparation_id) REFERENCES preparation_schedule(preparation_schedule_id);

ALTER TABLE preparation_user
    ADD CONSTRAINT fk_preparation_user FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE;

ALTER TABLE preparation_user
    ADD CONSTRAINT fk_next_preparation_user FOREIGN KEY (next_preparation_id) REFERENCES preparation_user(preparation_id);

