-- 题库与提交域初始化 - 表结构与基础演示数据

-- 题目主表
CREATE TABLE IF NOT EXISTS `problem` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键，雪花 ID',
    `title`           VARCHAR(255)    NOT NULL COMMENT '题目标题',
    `difficulty`      ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'EASY' COMMENT '难度',
    `statement_md`    LONGTEXT        NOT NULL COMMENT '题面 Markdown 内容',
    `time_limit_ms`   INT             NOT NULL DEFAULT 1000 COMMENT '时间限制（毫秒）',
    `memory_limit_kb` INT             NOT NULL DEFAULT 256000 COMMENT '内存限制（KB）',
    `status`          ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT '题目状态：草稿/已发布/已归档',
    `submit_count`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '提交总次数（可选统计字段）',
    `accepted_count`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '通过次数（可选统计字段）',
    `is_deleted`      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0 正常/1 删除',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT UNSIGNED          DEFAULT NULL COMMENT '创建人',
    `updated_by`      BIGINT UNSIGNED          DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_problem_status_difficulty` (`status`,`difficulty`),
    KEY `idx_problem_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目主表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id`        BIGINT UNSIGNED NOT NULL COMMENT '主键，雪花 ID',
    `name`      VARCHAR(64)     NOT NULL COMMENT '标签名称',
    `type`      VARCHAR(32)              DEFAULT NULL COMMENT '标签类型（如 ALGO/DS/COURSE 等）',
    `created_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目标签';

-- 题目-标签 多对多关系
CREATE TABLE IF NOT EXISTS `problem_tag` (
    `id`         BIGINT UNSIGNED NOT NULL COMMENT '主键，雪花 ID',
    `problem_id` BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
    `tag_id`     BIGINT UNSIGNED NOT NULL COMMENT '标签 ID',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_tag` (`problem_id`,`tag_id`),
    KEY `idx_problem_tag_problem` (`problem_id`),
    KEY `idx_problem_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-标签关联';

-- 提交表（判题第一阶段为 stub，仅记录基本信息）
CREATE TABLE IF NOT EXISTS `submission` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键，雪花 ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '提交用户 ID',
    `problem_id`  BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
    `language`    VARCHAR(32)     NOT NULL COMMENT '代码语言，如 C++/Java/Python3 等',
    `source_code` MEDIUMTEXT      NOT NULL COMMENT '源代码内容',
    `status`      ENUM('QUEUED','RUNNING','AC','WA','TLE','MLE','RE','CE','SYSTEM_ERROR') NOT NULL DEFAULT 'QUEUED' COMMENT '判题状态（stub 阶段仅使用 QUEUED 或手动更新）',
    `time_ms`     INT                      DEFAULT NULL COMMENT '运行时间（毫秒）',
    `memory_kb`   INT                      DEFAULT NULL COMMENT '内存消耗（KB）',
    `compile_message` TEXT                DEFAULT NULL COMMENT '编译信息（可选）',
    `judge_message`   TEXT                DEFAULT NULL COMMENT '判题信息（可选）',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_submission_user_problem` (`user_id`,`problem_id`,`created_at`),
    KEY `idx_submission_problem` (`problem_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录表（stub 阶段）';

-- 用户-题目进度表（用于快速查询做题状态）
CREATE TABLE IF NOT EXISTS `user_problem_progress` (
    `id`               BIGINT UNSIGNED NOT NULL COMMENT '主键，雪花 ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `problem_id`       BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
    `status`           ENUM('UNSOLVED','ATTEMPTED','SOLVED') NOT NULL DEFAULT 'UNSOLVED' COMMENT '做题状态',
    `last_submission_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近一次提交 ID',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_problem` (`user_id`,`problem_id`),
    KEY `idx_user_problem_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-题目进度';

-- 基础演示数据：标签
INSERT INTO `tag` (`id`, `name`, `type`, `created_at`, `updated_at`)
VALUES
    (2000000000000000001, '数组', 'ALGO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2000000000000000002, '哈希表', 'ALGO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2000000000000000003, '链表', 'DS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2000000000000000004, '滑动窗口', 'ALGO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `type` = VALUES(`type`),
    `updated_at` = CURRENT_TIMESTAMP;

-- 基础演示数据：题目（示例：两数之和 等）
INSERT INTO `problem` (
    `id`, `title`, `difficulty`, `statement_md`,
    `time_limit_ms`, `memory_limit_kb`, `status`,
    `submit_count`, `accepted_count`,
    `is_deleted`, `created_at`, `updated_at`, `created_by`, `updated_by`
) VALUES
    (
        2100000000000000001,
        '两数之和',
        'EASY',
        '# 两数之和\n\n给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出 **和为目标值** `target` 的那 **两个** 整数，并返回它们的数组下标。\n\n你可以假设每种输入只会对应一个答案。但是，数组中同一个元素在答案里不能重复出现。\n\n你可以按任意顺序返回答案。\n\n**示例：**\n\n- 输入：`nums = [2,7,11,15]`, `target = 9`\n- 输出：`[0,1]`\n\n**提示：**\n\n- `2 <= nums.length <= 10^4`\n- `-10^9 <= nums[i] <= 10^9`\n- `-10^9 <= target <= 10^9`\n- 只会存在一个有效答案\n',
        1000,
        256000,
        'PUBLISHED',
        0,
        0,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        1998338632572506116, -- 示例创建人：test_teacher
        1998338632572506116
    ),
    (
        2100000000000000002,
        '两数相加',
        'MEDIUM',
        '# 两数相加\n\n给你两个 **非空** 的链表，表示两个非负的整数。它们每位数字都是按照 **逆序** 方式存储的，并且每个节点只能存储 **一位** 数字。\n\n请你将两个数相加，并以相同形式返回一个表示和的链表。\n\n你可以假设除了数字 0 之外，这两个数都不会以 0 开头。\n',
        2000,
        256000,
        'PUBLISHED',
        0,
        0,
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        1998338632572506116,
        1998338632572506116
    )
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `difficulty` = VALUES(`difficulty`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

-- 题目-标签 关联
INSERT INTO `problem_tag` (`id`, `problem_id`, `tag_id`, `created_at`)
VALUES
    (2200000000000000001, 2100000000000000001, 2000000000000000001, CURRENT_TIMESTAMP), -- 两数之和 - 数组
    (2200000000000000002, 2100000000000000001, 2000000000000000002, CURRENT_TIMESTAMP), -- 两数之和 - 哈希表
    (2200000000000000003, 2100000000000000002, 2000000000000000003, CURRENT_TIMESTAMP)  -- 两数相加 - 链表
;

