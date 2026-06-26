ALTER TABLE preparation_user
    MODIFY COLUMN preparation_name VARCHAR(50) NOT NULL;

ALTER TABLE preparation_schedule
    MODIFY COLUMN preparation_name VARCHAR(50) NOT NULL;
