-- Learner profile narrative and observable adaptive updates.
ALTER TABLE user_profile
  ADD COLUMN profile_narrative TEXT NULL COMMENT 'Natural-language learner self description' AFTER learning_goal,
  ADD COLUMN adaptive_summary TEXT NULL COMMENT 'System-maintained learner profile summary' AFTER profile_narrative,
  ADD COLUMN last_activity_source VARCHAR(64) NULL COMMENT 'Latest profile update source' AFTER profile_source,
  ADD COLUMN last_activity_summary VARCHAR(1000) NULL COMMENT 'Latest learning activity summary' AFTER last_activity_source,
  ADD COLUMN last_activity_at DATETIME NULL COMMENT 'Latest learning activity time' AFTER last_activity_summary;

-- Per-option explanations shown after a single-choice quiz is submitted.
ALTER TABLE quiz_question
  ADD COLUMN option_analysis_json JSON NULL COMMENT 'Per-option correctness explanations' AFTER analysis_text;
