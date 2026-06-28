DELETE urt
FROM user_refresh_token urt
JOIN `user` u ON u.user_id = urt.user_id
WHERE u.social_type = 'APPLE'
  AND u.social_id = 'bench-apple-user';

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
) VALUES (
  'bench.apple@example.com',
  NULL,
  NULL,
  'Bench User',
  10,
  NULL,
  100.0,
  0,
  0,
  'USER',
  'APPLE',
  'bench-apple-user',
  NULL,
  NULL,
  'bench-existing-provider-grant',
  NULL
) ON DUPLICATE KEY UPDATE
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
