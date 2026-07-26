-- 修复点赞/收藏唯一约束并校准冗余计数。
-- 该脚本会删除重复交互记录并重算计数，建议在低峰期执行并提前备份。

DELETE duplicate_record
FROM user_picture_interaction duplicate_record
JOIN user_picture_interaction retained_record
  ON duplicate_record.userId = retained_record.userId
 AND duplicate_record.pictureId = retained_record.pictureId
 AND duplicate_record.type = retained_record.type
 AND duplicate_record.albumId <=> retained_record.albumId
 AND duplicate_record.id > retained_record.id;

DELIMITER //

DROP PROCEDURE IF EXISTS ensure_user_picture_interaction_unique_key //
CREATE PROCEDURE ensure_user_picture_interaction_unique_key()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_picture_interaction'
          AND COLUMN_NAME = 'albumIdKey'
    ) THEN
        ALTER TABLE user_picture_interaction
            ADD COLUMN albumIdKey BIGINT
                GENERATED ALWAYS AS (COALESCE(albumId, 0)) STORED
                COMMENT '收藏夹ID唯一键归一化值'
                AFTER albumId;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_picture_interaction'
          AND INDEX_NAME = 'uniq_user_picture_type_album'
    ) THEN
        ALTER TABLE user_picture_interaction
            DROP INDEX uniq_user_picture_type_album;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_picture_interaction'
          AND INDEX_NAME = 'uniq_user_picture_type_album_key'
    ) THEN
        ALTER TABLE user_picture_interaction
            ADD CONSTRAINT uniq_user_picture_type_album_key
                UNIQUE (userId, pictureId, type, albumIdKey);
    END IF;
END //

CALL ensure_user_picture_interaction_unique_key() //
DROP PROCEDURE ensure_user_picture_interaction_unique_key //

DELIMITER ;

UPDATE picture p
LEFT JOIN (
    SELECT pictureId,
           SUM(type = 0) AS likeCount,
           SUM(type = 1) AS favoriteCount
    FROM user_picture_interaction
    GROUP BY pictureId
) interaction_count ON interaction_count.pictureId = p.id
SET p.likeCount = COALESCE(interaction_count.likeCount, 0),
    p.favoriteCount = COALESCE(interaction_count.favoriteCount, 0);

UPDATE picture_album album
LEFT JOIN (
    SELECT albumId, COUNT(*) AS pictureCount
    FROM user_picture_interaction
    WHERE type = 1
      AND albumId IS NOT NULL
    GROUP BY albumId
) interaction_count ON interaction_count.albumId = album.id
SET album.pictureCount = COALESCE(interaction_count.pictureCount, 0);
