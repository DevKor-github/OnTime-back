ALTER TABLE preparation_user
    ADD COLUMN order_index INT NULL;

ALTER TABLE preparation_schedule
    ADD COLUMN order_index INT NULL;

SET @prev_user_id := NULL;
SET @user_order := -1;
UPDATE preparation_user pu
JOIN (
    SELECT preparation_user_id,
           user_id,
           @user_order := IF(@prev_user_id = user_id, @user_order + 1, 0) AS computed_order,
           @prev_user_id := user_id
    FROM preparation_user
    ORDER BY user_id, preparation_user_id
) ordered ON ordered.preparation_user_id = pu.preparation_user_id
SET pu.order_index = ordered.computed_order;

SET @prev_schedule_id := NULL;
SET @schedule_order := -1;
UPDATE preparation_schedule ps
JOIN (
    SELECT preparation_schedule_id,
           schedule_id,
           @schedule_order := IF(@prev_schedule_id = schedule_id, @schedule_order + 1, 0) AS computed_order,
           @prev_schedule_id := schedule_id
    FROM preparation_schedule
    ORDER BY schedule_id, preparation_schedule_id
) ordered ON ordered.preparation_schedule_id = ps.preparation_schedule_id
SET ps.order_index = ordered.computed_order;

CREATE TABLE preparation_template (
    preparation_template_id BINARY(16) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_name VARCHAR(30) NOT NULL,
    normalized_template_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_preparation_template_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_preparation_template_user_deleted ON preparation_template(user_id, deleted_at);
CREATE INDEX idx_preparation_template_created ON preparation_template(created_at);
CREATE UNIQUE INDEX uk_preparation_template_active_name ON preparation_template(user_id, normalized_template_name, deleted_at);

CREATE TABLE preparation_template_step (
    preparation_template_step_id BINARY(16) PRIMARY KEY,
    preparation_template_id BINARY(16) NOT NULL,
    preparation_name VARCHAR(50) NOT NULL,
    preparation_time INT NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT fk_preparation_template_step_template FOREIGN KEY (preparation_template_id) REFERENCES preparation_template (preparation_template_id) ON DELETE CASCADE
);

CREATE INDEX idx_preparation_template_step_template_order ON preparation_template_step(preparation_template_id, order_index);

ALTER TABLE schedule
    ADD COLUMN preparation_mode VARCHAR(20) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN preparation_template_id BINARY(16) NULL,
    ADD CONSTRAINT fk_schedule_preparation_template FOREIGN KEY (preparation_template_id) REFERENCES preparation_template (preparation_template_id) ON DELETE SET NULL;

UPDATE schedule
SET preparation_mode = CASE
    WHEN is_change = 1 THEN 'CUSTOM'
    ELSE 'DEFAULT'
END;
