-- Review data retention requirements before running this rollback.

DROP TABLE IF EXISTS `admin_operation_log`;
DROP INDEX `idx_user_role_status` ON `user`;
DROP INDEX `idx_user_last_login_time` ON `user`;
ALTER TABLE `user`
    DROP COLUMN `lastLoginTime`,
    DROP COLUMN `userStatus`;
