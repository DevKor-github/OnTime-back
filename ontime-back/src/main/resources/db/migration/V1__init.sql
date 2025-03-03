CREATE TABLE api_log (
                         api_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         request_url VARCHAR(255),
                         request_method VARCHAR(255),
                         user_id VARCHAR(255),
                         client_ip VARCHAR(255),
                         response_status INT,
                         taken_time BIGINT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user (
                      user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      email VARCHAR(255),
                      password VARCHAR(255),
                      image_url VARCHAR(255),
                      name VARCHAR(30),
                      spare_time INT,
                      note TEXT,
                      punctuality_score FLOAT,
                      schedule_count_after_reset INT,
                      lateness_count_after_reset INT,
                      role VARCHAR(255),
                      social_type VARCHAR(255),
                      social_id VARCHAR(255),
                      refresh_token VARCHAR(255),
                      firebase_token VARCHAR(255),
                      social_login_token VARCHAR(255)
);

CREATE TABLE place (
                       place_id BINARY(16) PRIMARY KEY,
                       place_name VARCHAR(255)
);

CREATE TABLE schedule (
                          schedule_id BINARY(16) PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          place_id BINARY(16),
                          schedule_name VARCHAR(30) NOT NULL,
                          move_time INT,
                          schedule_time TIMESTAMP,
                          is_change TINYINT(1),
                          is_started TINYINT(1),
                          schedule_spare_time INT,
                          lateness_time INT,
                          schedule_note TEXT,
                          CONSTRAINT fk_schedule_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_schedule_place FOREIGN KEY (place_id) REFERENCES place (place_id) ON DELETE SET NULL
);

CREATE INDEX idx_schedule_user_id ON schedule(user_id);
CREATE INDEX idx_schedule_time ON schedule(schedule_time);

CREATE TABLE feedback (
                          feedback_id BINARY(16) PRIMARY KEY,
                          message VARCHAR(255),
                          create_at TIMESTAMP,
                          user_id BIGINT NOT NULL,
                          CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE TABLE friend_ship (
                             friend_ship_id BINARY(16) PRIMARY KEY,
                             requester_id BIGINT NOT NULL,
                             receiver_id BIGINT,
                             accept_status VARCHAR(255),
                             CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES user (user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_friendship_receiver FOREIGN KEY (receiver_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE TABLE preparation_schedule (
                                      preparation_schedule_id BINARY(16) PRIMARY KEY,
                                      schedule_id BINARY(16) NOT NULL,
                                      preparation_name VARCHAR(30) NOT NULL,
                                      preparation_time INT,
                                      next_preparation_id BINARY(16),
                                      CONSTRAINT fk_preparation_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (schedule_id) ON DELETE CASCADE,
                                      CONSTRAINT fk_next_preparation_schedule FOREIGN KEY (next_preparation_id) REFERENCES preparation_schedule (preparation_schedule_id) ON DELETE SET NULL
);

CREATE TABLE preparation_user (
                                  preparation_user_id BINARY(16) PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  preparation_name VARCHAR(30) NOT NULL,
                                  preparation_time INT,
                                  next_preparation_id BINARY(16),
                                  CONSTRAINT fk_preparation_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
                                  CONSTRAINT fk_next_preparation_user FOREIGN KEY (next_preparation_id) REFERENCES preparation_user (preparation_user_id) ON DELETE SET NULL
);

CREATE TABLE user_setting (
                              user_setting_id BINARY(16) PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              is_notifications_enabled TINYINT(1) NOT NULL DEFAULT 1,
                              sound_volume INT NOT NULL DEFAULT 50,
                              is_play_on_speaker TINYINT(1) NOT NULL DEFAULT 1,
                              is24hour_format TINYINT(1) NOT NULL DEFAULT 1,
                              CONSTRAINT fk_user_setting FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);
