-- 为现有 AI 生成历史补充任务类型和对象级权限查询所需的唯一约束。
-- 旧记录默认属于文生图任务（taskType = 0）。

DELIMITER //

DROP PROCEDURE IF EXISTS add_ai_generation_task_ownership_columns //
CREATE PROCEDURE add_ai_generation_task_ownership_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_gen_history'
          AND COLUMN_NAME = 'taskType'
    ) THEN
        ALTER TABLE ai_gen_history
            ADD COLUMN taskType TINYINT NOT NULL DEFAULT 0
                COMMENT 'AI任务类型：0-文生图，1-扩图'
                AFTER taskId;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_gen_history'
          AND COLUMN_NAME = 'sourcePictureId'
    ) THEN
        ALTER TABLE ai_gen_history
            ADD COLUMN sourcePictureId BIGINT NULL
                COMMENT '扩图任务的源图片ID'
                AFTER taskType;
    END IF;
END //

CALL add_ai_generation_task_ownership_columns() //
DROP PROCEDURE add_ai_generation_task_ownership_columns //

DELIMITER ;

DELIMITER //

DROP PROCEDURE IF EXISTS add_ai_generation_task_ownership_unique_key //
CREATE PROCEDURE add_ai_generation_task_ownership_unique_key()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_gen_history'
          AND INDEX_NAME = 'uniq_ai_gen_task_type_task_id'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM ai_gen_history
            WHERE taskId IS NOT NULL
            GROUP BY taskType, taskId
            HAVING COUNT(*) > 1
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'ai_gen_history 存在重复 taskId，请先人工确认并清理';
        END IF;

        ALTER TABLE ai_gen_history
            ADD CONSTRAINT uniq_ai_gen_task_type_task_id
                UNIQUE (taskType, taskId);
    END IF;
END //

CALL add_ai_generation_task_ownership_unique_key() //
DROP PROCEDURE add_ai_generation_task_ownership_unique_key //

DELIMITER ;
