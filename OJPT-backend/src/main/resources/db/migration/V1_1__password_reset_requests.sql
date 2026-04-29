CREATE TABLE IF NOT EXISTS `password_reset_request` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Target user id',
    `account_identifier` VARCHAR(128) NOT NULL COMMENT 'Submitted username or email',
    `status` ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT 'Review status',
    `reviewed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Admin reviewer id',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT 'Review time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    KEY `idx_password_reset_request_status_created` (`status`, `created_at`),
    KEY `idx_password_reset_request_user_status` (`user_id`, `status`),
    KEY `idx_password_reset_request_reviewer` (`reviewed_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Password reset approval requests';
