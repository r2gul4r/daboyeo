SET @stmt = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_runs'
      AND CONSTRAINT_NAME = 'fk_recommendation_runs_profile'
  ),
  'SELECT 1',
  'ALTER TABLE recommendation_runs ADD CONSTRAINT fk_recommendation_runs_profile FOREIGN KEY (anonymous_id) REFERENCES recommendation_profiles(anonymous_id) ON DELETE CASCADE'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_feedback'
      AND CONSTRAINT_NAME = 'fk_recommendation_feedback_run'
  ),
  'SELECT 1',
  'ALTER TABLE recommendation_feedback ADD CONSTRAINT fk_recommendation_feedback_run FOREIGN KEY (run_id) REFERENCES recommendation_runs(run_id) ON DELETE CASCADE'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_feedback'
      AND CONSTRAINT_NAME = 'fk_recommendation_feedback_profile'
  ),
  'SELECT 1',
  'ALTER TABLE recommendation_feedback ADD CONSTRAINT fk_recommendation_feedback_profile FOREIGN KEY (anonymous_id) REFERENCES recommendation_profiles(anonymous_id) ON DELETE CASCADE'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
