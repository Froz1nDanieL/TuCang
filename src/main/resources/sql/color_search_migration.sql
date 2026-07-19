-- 可重复执行，兼容数据库中已经存在部分颜色字段的情况。
DELIMITER //

DROP PROCEDURE IF EXISTS ensure_picture_color_search_columns //
CREATE PROCEDURE ensure_picture_color_search_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture' AND COLUMN_NAME = 'picColor'
    ) THEN
        ALTER TABLE picture ADD COLUMN picColor varchar(16) NULL COMMENT '图片主色调' AFTER favoriteCount;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture' AND COLUMN_NAME = 'colorPalette'
    ) THEN
        ALTER TABLE picture ADD COLUMN colorPalette json NULL COMMENT 'Lab 调色板 JSON' AFTER picColor;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture' AND COLUMN_NAME = 'colorTags'
    ) THEN
        ALTER TABLE picture ADD COLUMN colorTags json NULL COMMENT '十种标准色标签 JSON' AFTER colorPalette;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture' AND COLUMN_NAME = 'colorScores'
    ) THEN
        ALTER TABLE picture ADD COLUMN colorScores json NULL COMMENT '十种标准色离线分数 JSON' AFTER colorTags;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'picture' AND COLUMN_NAME = 'colorAlgoVersion'
    ) THEN
        ALTER TABLE picture ADD COLUMN colorAlgoVersion int DEFAULT 1 NULL COMMENT '颜色算法版本' AFTER colorScores;
    END IF;
END //

CALL ensure_picture_color_search_columns() //
DROP PROCEDURE ensure_picture_color_search_columns //

DELIMITER ;
