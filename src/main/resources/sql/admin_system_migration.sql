-- TuCang administration system migration.
-- Apply explicitly after taking a database backup.

ALTER TABLE `user`
    ADD COLUMN `userStatus` tinyint NOT NULL DEFAULT 0 COMMENT '账号状态：0-正常，1-禁用' AFTER `userRole`,
    ADD COLUMN `lastLoginTime` datetime NULL COMMENT '最近登录时间' AFTER `userStatus`;

CREATE INDEX `idx_user_role_status` ON `user` (`userRole`, `userStatus`);
CREATE INDEX `idx_user_last_login_time` ON `user` (`lastLoginTime`);

CREATE TABLE `admin_operation_log`
(
    `id`             bigint       NOT NULL COMMENT '主键',
    `operatorId`     bigint       NULL COMMENT '操作人 ID',
    `operatorName`   varchar(256) NULL COMMENT '操作人名称',
    `operatorRole`   varchar(32)  NULL COMMENT '操作人角色',
    `module`         varchar(64)  NOT NULL COMMENT '业务模块',
    `action`         varchar(64)  NOT NULL COMMENT '操作动作',
    `targetType`     varchar(64)  NULL COMMENT '目标类型',
    `targetId`       varchar(128) NULL COMMENT '目标 ID',
    `requestMethod`  varchar(16)  NOT NULL COMMENT 'HTTP 方法',
    `requestPath`    varchar(512) NOT NULL COMMENT '请求路径',
    `requestParams`  text         NULL COMMENT '脱敏后的请求摘要',
    `resultCode`     int          NOT NULL COMMENT '业务结果码',
    `success`        tinyint      NOT NULL COMMENT '是否成功',
    `errorMessage`   varchar(512) NULL COMMENT '错误摘要',
    `ip`             varchar(64)  NULL COMMENT '客户端 IP',
    `userAgent`      varchar(512) NULL COMMENT 'User-Agent',
    `durationMs`     bigint       NOT NULL COMMENT '耗时毫秒',
    `createTime`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_admin_log_operator_time` (`operatorId`, `createTime`),
    INDEX `idx_admin_log_module_action_time` (`module`, `action`, `createTime`),
    INDEX `idx_admin_log_success_time` (`success`, `createTime`)
) COMMENT '后台管理操作审计日志' COLLATE = utf8mb4_unicode_ci;
