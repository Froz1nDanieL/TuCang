-- TuCang administration phase 2 P0 migration.
-- Apply explicitly after taking a database backup. The application never runs this file automatically.

ALTER TABLE `picture`
    ADD COLUMN `sourceType` varchar(32) NOT NULL DEFAULT 'UNKNOWN'
        COMMENT '来源：UNKNOWN/LOCAL_UPLOAD/URL_UPLOAD/AI_TEXT/AI_OUTPAINT' AFTER `spaceId`,
    ADD COLUMN `aiTaskId` varchar(255) NULL COMMENT '关联 AI 外部任务 ID' AFTER `sourceType`;

CREATE INDEX `idx_picture_source_review_time`
    ON `picture` (`sourceType`, `reviewStatus`, `createTime`);
CREATE INDEX `idx_picture_ai_task_id` ON `picture` (`aiTaskId`);

ALTER TABLE `ai_gen_history`
    ADD COLUMN `taskStatus` varchar(16) NOT NULL DEFAULT 'UNKNOWN'
        COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED/UNKNOWN' AFTER `status`,
    ADD COLUMN `modelName` varchar(128) NULL COMMENT '实际模型名称' AFTER `taskStatus`,
    ADD COLUMN `requestId` varchar(255) NULL COMMENT '供应商请求 ID' AFTER `modelName`,
    ADD COLUMN `requestParams` text NULL COMMENT '脱敏后的请求参数' AFTER `requestId`,
    ADD COLUMN `completedTime` datetime NULL COMMENT '完成时间' AFTER `requestParams`,
    ADD COLUMN `durationMs` bigint NULL COMMENT '任务耗时毫秒' AFTER `completedTime`,
    ADD COLUMN `resultCount` int NOT NULL DEFAULT 0 COMMENT '结果数量' AFTER `durationMs`,
    ADD COLUMN `errorCode` varchar(128) NULL COMMENT '错误码' AFTER `resultCount`,
    ADD COLUMN `errorMessage` varchar(512) NULL COMMENT '脱敏错误摘要' AFTER `errorCode`,
    ADD COLUMN `retryFromTaskId` varchar(255) NULL COMMENT '重试来源任务 ID' AFTER `errorMessage`;

UPDATE `ai_gen_history`
SET `taskStatus` = CASE
    WHEN `imageUrl` IS NOT NULL AND LENGTH(TRIM(`imageUrl`)) > 0 THEN 'SUCCEEDED'
    ELSE 'UNKNOWN'
END;

CREATE INDEX `idx_ai_task_status_time`
    ON `ai_gen_history` (`taskStatus`, `createTime`);
CREATE INDEX `idx_ai_model_type_time`
    ON `ai_gen_history` (`modelName`, `taskType`, `createTime`);
CREATE INDEX `idx_ai_retry_from_task` ON `ai_gen_history` (`retryFromTaskId`);

CREATE TABLE `picture_review_record`
(
    `id`             bigint       NOT NULL COMMENT '主键',
    `pictureId`      bigint       NOT NULL COMMENT '图片 ID',
    `fromStatus`     int          NOT NULL COMMENT '原审核状态',
    `toStatus`       int          NOT NULL COMMENT '新审核状态',
    `reviewerId`     bigint       NOT NULL COMMENT '审核人 ID',
    `reviewerRole`   varchar(32)  NOT NULL COMMENT '审核人角色',
    `reasonCode`     varchar(64)  NULL COMMENT '标准拒绝原因',
    `reviewMessage`  varchar(512) NULL COMMENT '审核意见',
    `durationMs`     bigint       NULL COMMENT '从入队到处理完成耗时',
    `conflict`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否并发冲突',
    `createTime`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_review_picture_time` (`pictureId`, `createTime`),
    INDEX `idx_review_reviewer_time` (`reviewerId`, `createTime`),
    INDEX `idx_review_result_time` (`toStatus`, `createTime`)
) COMMENT '图片审核决策记录' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `picture_index_record`
(
    `id`              bigint       NOT NULL COMMENT '主键',
    `pictureId`       bigint       NULL COMMENT '图片 ID',
    `recordType`      varchar(16)  NOT NULL COMMENT 'CHECK/SYNC',
    `batchId`         varchar(64)  NULL COMMENT '对账批次 ID',
    `syncType`        varchar(32)  NULL COMMENT 'IMMEDIATE/MANUAL/FULL/INCREMENTAL',
    `operation`       varchar(16)  NULL COMMENT 'UPSERT/DELETE/CHECK',
    `mismatchTypes`   varchar(512) NULL COMMENT '不一致类型 JSON',
    `success`         tinyint      NOT NULL COMMENT '是否成功',
    `errorMessage`    varchar(512) NULL COMMENT '错误摘要',
    `durationMs`      bigint       NOT NULL DEFAULT 0,
    `resolved`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否已解决',
    `resolvedTime`    datetime     NULL,
    `operatorId`      bigint       NULL COMMENT '人工操作管理员',
    `createTime`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_index_picture_time` (`pictureId`, `createTime`),
    INDEX `idx_index_batch_result` (`batchId`, `success`),
    INDEX `idx_index_open_mismatch` (`recordType`, `resolved`, `createTime`)
) COMMENT '图片索引检查与同步记录' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `admin_job_execution`
(
    `id`              bigint       NOT NULL COMMENT '主键',
    `jobName`         varchar(64)  NOT NULL COMMENT '任务名',
    `triggerType`     varchar(16)  NOT NULL COMMENT 'MANUAL/SCHEDULED',
    `status`          varchar(16)  NOT NULL COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED',
    `scopeType`       varchar(16)  NULL COMMENT 'SAMPLE/FULL',
    `totalCount`      bigint       NOT NULL DEFAULT 0,
    `processedCount`  bigint       NOT NULL DEFAULT 0,
    `successCount`    bigint       NOT NULL DEFAULT 0,
    `failureCount`    bigint       NOT NULL DEFAULT 0,
    `errorMessage`    varchar(512) NULL,
    `operatorId`      bigint       NULL,
    `idempotencyKey`  varchar(128) NULL,
    `startedTime`     datetime     NULL,
    `completedTime`   datetime     NULL,
    `createTime`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_job_idempotency` (`idempotencyKey`),
    INDEX `idx_admin_job_name_status` (`jobName`, `status`, `createTime`)
) COMMENT '后台任务执行记录' COLLATE = utf8mb4_unicode_ci;
