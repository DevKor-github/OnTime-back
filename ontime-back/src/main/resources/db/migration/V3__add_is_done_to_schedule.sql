ALTER TABLE schedule ADD COLUMN done_status VARCHAR(255);

-- lateness_time > 0 → 'LATE'
UPDATE schedule
SET done_status = 'LATE'
WHERE lateness_time > 0;

-- lateness_time = 0 → 'NORMAL'
UPDATE schedule
SET done_status = 'NORMAL'
WHERE lateness_time = 0;

-- lateness_time = -1 → 'NOT_ENDED'
UPDATE schedule
SET done_status = 'NOT_ENDED'
WHERE lateness_time = -1;

-- lateness_time = -2 → 'ABNORMAL'
UPDATE schedule
SET done_status = 'ABNORMAL'
WHERE lateness_time = -2;