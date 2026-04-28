INSERT INTO `role` (`id`, `code`, `name`, `description`, `level`, `created_at`, `updated_at`)
SELECT 1000000000000000100, 'USER', '用户', '普通用户', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `code` = 'USER');

INSERT INTO `role` (`id`, `code`, `name`, `description`, `level`, `created_at`, `updated_at`)
SELECT 1000000000000000104, 'ADMIN', '管理员', '平台管理员', 500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `code` = 'ADMIN');

UPDATE `user`
SET `role_type` = CASE
    WHEN UPPER(COALESCE(`role_type`, 'USER')) = 'ADMIN' THEN 'ADMIN'
    ELSE 'USER'
END;

DELETE rp
FROM `role_permission` rp
INNER JOIN `role` r ON r.`id` = rp.`role_id`
WHERE r.`code` NOT IN ('USER', 'ADMIN');

DELETE ur
FROM `user_role` ur
INNER JOIN `role` r ON r.`id` = ur.`role_id`
WHERE r.`code` NOT IN ('USER', 'ADMIN');

DELETE FROM `role`
WHERE `code` NOT IN ('USER', 'ADMIN');

UPDATE `role`
SET `name` = '用户',
    `description` = '普通用户',
    `level` = 100
WHERE `code` = 'USER';

UPDATE `role`
SET `name` = '管理员',
    `description` = '平台管理员',
    `level` = 500
WHERE `code` = 'ADMIN';

DELETE FROM `user_role`;

SET @next_user_role_id := 1998338632572508000;

INSERT INTO `user_role` (`id`, `user_id`, `role_id`, `bind_source`, `created_at`, `updated_at`)
SELECT
    (@next_user_role_id := @next_user_role_id + 1),
    u.`id`,
    r.`id`,
    'SYSTEM_DUAL_ROLE_MIGRATION',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM `user` u
INNER JOIN `role` r ON r.`code` = u.`role_type`
WHERE u.`is_deleted` = 0;

ALTER TABLE `user`
MODIFY COLUMN `role_type` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER';
