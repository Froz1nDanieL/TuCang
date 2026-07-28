-- TuCang administration phase 2 P0 rollback.
-- Roll back application code first. This removes P0 history and cannot be undone without a backup.

DROP TABLE IF EXISTS `admin_job_execution`;
DROP TABLE IF EXISTS `picture_index_record`;
DROP TABLE IF EXISTS `picture_review_record`;

ALTER TABLE `ai_gen_history`
    DROP INDEX `idx_ai_retry_from_task`,
    DROP INDEX `idx_ai_model_type_time`,
    DROP INDEX `idx_ai_task_status_time`,
    DROP COLUMN `retryFromTaskId`,
    DROP COLUMN `errorMessage`,
    DROP COLUMN `errorCode`,
    DROP COLUMN `resultCount`,
    DROP COLUMN `durationMs`,
    DROP COLUMN `completedTime`,
    DROP COLUMN `requestParams`,
    DROP COLUMN `requestId`,
    DROP COLUMN `modelName`,
    DROP COLUMN `taskStatus`;

ALTER TABLE `picture`
    DROP INDEX `idx_picture_ai_task_id`,
    DROP INDEX `idx_picture_source_review_time`,
    DROP COLUMN `aiTaskId`,
    DROP COLUMN `sourceType`;
