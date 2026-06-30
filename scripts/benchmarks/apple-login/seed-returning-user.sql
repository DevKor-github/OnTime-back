DELETE urt
FROM user_refresh_token urt
JOIN `user` u ON u.user_id = urt.user_id
WHERE u.social_type = 'APPLE'
  AND u.social_id LIKE 'bench-apple-user-%';

INSERT INTO `user` (
  email,
  password,
  image_url,
  name,
  spare_time,
  note,
  punctuality_score,
  schedule_count_after_reset,
  lateness_count_after_reset,
  role,
  social_type,
  social_id,
  refresh_token,
  firebase_token,
  social_login_token,
  access_token
) SELECT
  CONCAT('bench.apple+', seq.n, '@example.com'),
  NULL,
  NULL,
  CONCAT('Bench User ', seq.n),
  10,
  NULL,
  100.0,
  0,
  0,
  'USER',
  'APPLE',
  CONCAT('bench-apple-user-', seq.n),
  NULL,
  NULL,
  'bench-existing-provider-grant',
  NULL
FROM (
  SELECT ones.n + tens.n * 10 + 1 AS n
  FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
  ) ones
  CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6
  ) tens
  WHERE ones.n + tens.n * 10 + 1 <= 64
) seq
ON DUPLICATE KEY UPDATE
  email = VALUES(email),
  name = VALUES(name),
  spare_time = VALUES(spare_time),
  note = VALUES(note),
  punctuality_score = VALUES(punctuality_score),
  schedule_count_after_reset = VALUES(schedule_count_after_reset),
  lateness_count_after_reset = VALUES(lateness_count_after_reset),
  role = VALUES(role),
  refresh_token = VALUES(refresh_token),
  firebase_token = VALUES(firebase_token),
  social_login_token = VALUES(social_login_token),
  access_token = VALUES(access_token);
