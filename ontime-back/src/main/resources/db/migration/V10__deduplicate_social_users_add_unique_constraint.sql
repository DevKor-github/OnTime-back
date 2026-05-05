CREATE TEMPORARY TABLE duplicate_social_user_keep AS
SELECT social_type, social_id, MAX(user_id) AS keep_user_id
FROM user
WHERE social_type IS NOT NULL
  AND social_id IS NOT NULL
GROUP BY social_type, social_id
HAVING COUNT(*) > 1;

DELETE u
FROM user u
JOIN duplicate_social_user_keep d
  ON u.social_type = d.social_type
 AND u.social_id = d.social_id
WHERE u.user_id <> d.keep_user_id;

DROP TEMPORARY TABLE duplicate_social_user_keep;

ALTER TABLE user
ADD CONSTRAINT uk_user_social_type_social_id UNIQUE (social_type, social_id);
