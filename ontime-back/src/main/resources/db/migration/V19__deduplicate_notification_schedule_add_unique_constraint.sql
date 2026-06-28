DELETE ns_duplicate
FROM notification_schedule ns_duplicate
JOIN notification_schedule ns_keep
  ON ns_duplicate.schedule_id = ns_keep.schedule_id
 AND ns_duplicate.id > ns_keep.id
WHERE ns_duplicate.schedule_id IS NOT NULL;

CREATE UNIQUE INDEX uk_notification_schedule_schedule
    ON notification_schedule (schedule_id);
