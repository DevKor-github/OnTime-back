CREATE TABLE notification_schedule (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       notification_time TIMESTAMP,
                                       is_sent BOOLEAN,
                                       schedule_id BINARY(16),
                                       CONSTRAINT fk_notification_schedule_schedule
                                          FOREIGN KEY (schedule_id)
                                          REFERENCES schedule (schedule_id)
                                          ON DELETE CASCADE
);