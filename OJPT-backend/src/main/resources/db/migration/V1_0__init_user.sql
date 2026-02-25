-- 用户与组织域初始化 - 仅表结构
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '登录名',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `email` VARCHAR(128) DEFAULT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用/1启用/2待审核',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `role_type` ENUM('USER','STUDENT','TEACHER','SCHOOL','ADMIN') NOT NULL DEFAULT 'USER',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0 正常/1 删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_user_status` (`status`),
    KEY `idx_user_role_type` (`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '角色编码，唯一',
    `name` VARCHAR(128) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `level` INT DEFAULT 0 COMMENT '角色层级/优先级',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色定义';

-- 用户-角色关系表
-- 外键说明：user_role.user_id -> user.id；user_role.role_id -> role.id
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `bind_source` VARCHAR(64) DEFAULT NULL COMMENT '绑定来源（手动/导入/系统）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户与角色多对多关系';

-- 权限表
CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `resource` VARCHAR(128) NOT NULL COMMENT '资源标识（接口/菜单/数据）',
    `action` VARCHAR(64) NOT NULL COMMENT '操作动作（GET/POST/DELETE... 或自定义）',
    `condition_json` JSON DEFAULT NULL COMMENT 'ABAC 条件，JSON 表达式',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_resource_action` (`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='细粒度权限表';

-- 角色-权限关联
-- 外键说明：role_permission.role_id -> role.id；role_permission.permission_id -> permission.id
CREATE TABLE IF NOT EXISTS `role_permission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
    KEY `idx_role_permission_role` (`role_id`),
    KEY `idx_role_permission_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与权限关系';

-- 用户扩展信息表
-- 外键说明：user_profile.user_id -> user.id（唯一约束保持 1:1）
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知/1男/2女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '现住址',
    `website` VARCHAR(255) DEFAULT NULL COMMENT '个人网站（博客或作品集等）',
    `github` VARCHAR(128) DEFAULT NULL COMMENT 'GitHub 用户名或链接',
    `company` VARCHAR(128) DEFAULT NULL COMMENT '所在公司',
    `position` VARCHAR(128) DEFAULT NULL COMMENT '职位',
    `skills` TEXT DEFAULT NULL COMMENT '技能（逗号分隔或 JSON）',
    `student_no` VARCHAR(64) DEFAULT NULL COMMENT '学号/工号',
    `school_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '学校ID',
    `bio` VARCHAR(255) DEFAULT NULL COMMENT '简介',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔或 JSON',
    `identity_status` TINYINT DEFAULT 0 COMMENT '实名/资质状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profile_user` (`user_id`),
    KEY `idx_user_profile_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展信息';

-- 学校
CREATE TABLE IF NOT EXISTS `school` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name` VARCHAR(255) NOT NULL COMMENT '学校名称',
    `contact` VARCHAR(128) DEFAULT NULL COMMENT '联系人/电话',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用/2待认证',
    `certified_at` DATETIME DEFAULT NULL COMMENT '认证时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_school_status` (`status`),
    KEY `idx_school_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校主体';

-- 院系/训练营
-- 外键说明：department.school_id -> school.id
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `school_id` BIGINT UNSIGNED NOT NULL COMMENT '学校ID',
    `name` VARCHAR(128) NOT NULL COMMENT '名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_department_school` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系/训练营';

-- 班级/小组
-- 外键说明：class.department_id -> department.id；class.teacher_id -> user.id（教师）
CREATE TABLE IF NOT EXISTS `class` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `department_id` BIGINT UNSIGNED NOT NULL COMMENT '院系ID',
    `name` VARCHAR(128) NOT NULL COMMENT '班级名称',
    `year` VARCHAR(16) DEFAULT NULL COMMENT '届/年份',
    `teacher_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '班主任/负责人',
    `merk` VARCHAR(32) DEFAULT NULL COMMENT '班级/小组类型/简介',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_class_department` (`department_id`),
    KEY `idx_class_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级/小组';

-- 学员加入班级关系
-- 外键说明：class_user.class_id -> class.id；class_user.user_id -> user.id（学员）；class_user.reviewer_id -> user.id（审核人）
CREATE TABLE IF NOT EXISTS `class_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `class_id` BIGINT UNSIGNED NOT NULL COMMENT '班级ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '学员ID',
    `join_type` ENUM('INVITE','APPLY') DEFAULT NULL COMMENT '加入方式：INVITE 邀请 / APPLY 申请',
    `join_status` ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '申请/邀请状态：PENDING 待审核/APPROVED 同意/REJECTED 拒绝',
    `join_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间（通过时记录）',
    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人/操作人',
    `review_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_user` (`class_id`,`user_id`),
    KEY `idx_class_user_class_status` (`class_id`,`join_status`),
    KEY `idx_class_user_user_status` (`user_id`,`join_status`),
    KEY `idx_class_user_reviewer` (`reviewer_id`),
    CONSTRAINT `fk_class_user_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级-学员关系';

-- 班级与教师关系
-- 外键说明：class_teacher.class_id -> class.id；class_teacher.teacher_id -> user.id（教师/助教）
CREATE TABLE IF NOT EXISTS `class_teacher` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `class_id` BIGINT UNSIGNED NOT NULL COMMENT '班级ID',
    `teacher_id` BIGINT UNSIGNED NOT NULL COMMENT '教师ID',
    `role` VARCHAR(32) DEFAULT NULL COMMENT '角色：班主任/助教等',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_teacher` (`class_id`,`teacher_id`),
    KEY `idx_class_teacher_class` (`class_id`),
    KEY `idx_class_teacher_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级-教师关系';
