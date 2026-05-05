DELETE u
FROM user u
JOIN (
    SELECT * FROM (
        SELECT social_type, social_id, MAX(user_id) AS keep_user_id
        FROM user
        WHERE social_type IS NOT NULL
          AND social_id IS NOT NULL
        GROUP BY social_type, social_id
        HAVING COUNT(*) > 1
    ) duplicate_groups
) d
  ON u.social_type = d.social_type
 AND u.social_id = d.social_id
WHERE u.user_id <> d.keep_user_id;

SET @constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'user'
      AND constraint_name = 'uk_user_social_type_social_id'
);

SET @add_constraint_sql = IF(
    @constraint_exists = 0,
    'ALTER TABLE user ADD CONSTRAINT uk_user_social_type_social_id UNIQUE (social_type, social_id)',
    'SELECT 1'
);

PREPARE add_constraint_statement FROM @add_constraint_sql;
EXECUTE add_constraint_statement;
DEALLOCATE PREPARE add_constraint_statement;
