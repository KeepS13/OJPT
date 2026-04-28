SET @admin_id := 1998338632572506113;
SET @admin1_id := 1998338632572506114;
SET @user_id := 1998338632572506117;
SET @user1_id := 1998338632572506121;
SET @role_user_id := 1000000000000000100;
SET @role_admin_id := 1000000000000000104;

-- Keep only the four demo accounts requested by product.
DELETE FROM `class_user`
WHERE `user_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

UPDATE `class_user`
SET `reviewer_id` = @admin1_id
WHERE `reviewer_id` = 1998338632572506116;

UPDATE `class_user`
SET `reviewer_id` = @admin_id
WHERE `reviewer_id` = 1998338632572506120;

DELETE FROM `class_user`
WHERE `reviewer_id` IS NOT NULL
  AND `reviewer_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

UPDATE `class`
SET `teacher_id` = @admin1_id
WHERE `teacher_id` = 1998338632572506116;

UPDATE `class`
SET `teacher_id` = @admin_id
WHERE `teacher_id` = 1998338632572506120;

UPDATE `class`
SET `teacher_id` = NULL
WHERE `teacher_id` IS NOT NULL
  AND `teacher_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

DELETE FROM `class_teacher`;

INSERT INTO `class_teacher` (`id`, `class_id`, `teacher_id`, `role`, `created_at`)
VALUES
    (1998338632572507401, 1998338632572507201, @admin1_id, '班主任', CURRENT_TIMESTAMP),
    (1998338632572507402, 1998338632572507202, @admin_id, '班主任', CURRENT_TIMESTAMP),
    (1998338632572507403, 1998338632572507206, @admin1_id, '助教', CURRENT_TIMESTAMP),
    (1998338632572507404, 1998338632572507210, @admin_id, '助教', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `class_id` = VALUES(`class_id`),
    `teacher_id` = VALUES(`teacher_id`),
    `role` = VALUES(`role`);

DELETE FROM `submission`
WHERE `user_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

DELETE FROM `user_problem_progress`
WHERE `user_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

DELETE FROM `user_role`;

DELETE FROM `user_profile`
WHERE `user_id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

DELETE FROM `user`
WHERE `email` IN ('admin@qq.com', 'admin1@qq.com', 'user@qq.com', 'user1@qq.com')
  AND `id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

DELETE FROM `user`
WHERE `id` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

INSERT INTO `user`
(`id`, `username`, `email`, `phone`, `avatar`, `password`, `status`, `role_type`, `is_deleted`, `created_at`, `updated_at`, `created_by`, `updated_by`)
VALUES
    (@admin_id, 'admin', 'admin@qq.com', '13800000001', '/avatars/admin.svg', '$2a$10$gZZ88Afdd4RAIp/XU8xBZuE7xOquiUhkJ7bnJLSjDKoBXBszFk/Pq', 1, 'ADMIN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (@admin1_id, 'admin1', 'admin1@qq.com', '13800000002', '/avatars/admin1.svg', '$2a$10$gZZ88Afdd4RAIp/XU8xBZuE7xOquiUhkJ7bnJLSjDKoBXBszFk/Pq', 1, 'ADMIN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (@user_id, 'user', 'user@qq.com', '13800000003', '/avatars/user.svg', '$2a$10$gZZ88Afdd4RAIp/XU8xBZuE7xOquiUhkJ7bnJLSjDKoBXBszFk/Pq', 1, 'USER', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (@user1_id, 'user1', 'user1@qq.com', '13800000004', '/avatars/user1.svg', '$2a$10$gZZ88Afdd4RAIp/XU8xBZuE7xOquiUhkJ7bnJLSjDKoBXBszFk/Pq', 1, 'USER', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE
    `username` = VALUES(`username`),
    `email` = VALUES(`email`),
    `phone` = VALUES(`phone`),
    `avatar` = VALUES(`avatar`),
    `password` = VALUES(`password`),
    `status` = VALUES(`status`),
    `role_type` = VALUES(`role_type`),
    `is_deleted` = VALUES(`is_deleted`),
    `updated_at` = CURRENT_TIMESTAMP,
    `updated_by` = VALUES(`updated_by`);

INSERT INTO `user_role` (`id`, `user_id`, `role_id`, `bind_source`, `created_at`, `updated_at`)
VALUES
    (1998338632572508001, @admin_id, @role_admin_id, 'SYSTEM_USER_SEED_CLEANUP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572508002, @admin1_id, @role_admin_id, 'SYSTEM_USER_SEED_CLEANUP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572508003, @user_id, @role_user_id, 'SYSTEM_USER_SEED_CLEANUP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572508004, @user1_id, @role_user_id, 'SYSTEM_USER_SEED_CLEANUP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `role_id` = VALUES(`role_id`),
    `bind_source` = VALUES(`bind_source`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `user_profile`
(`id`, `user_id`, `gender`, `birthday`, `address`, `github`, `company`, `position`, `skills`, `student_no`, `school_id`, `bio`, `tags`, `identity_status`, `created_at`, `updated_at`)
VALUES
    (1998338632572506200, @admin_id, 1, '1990-01-01', '北京市海淀区', 'admin', 'OJPT', '平台管理员', 'Java,Spring Boot,MySQL', 'A0001', 1998338632572507001, '负责平台日常维护', 'admin,ops', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572506201, @admin1_id, 2, '1992-02-02', '上海市浦东新区', 'admin1', 'OJPT', '系统管理员', 'Vue,TypeScript,测试', 'A0002', 1998338632572507005, '负责内容和用户管理', 'admin,content', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572506204, @user_id, 1, '2002-03-03', '浙江省杭州市', 'user', NULL, '学生', 'Java,算法基础', '2024001', 1998338632572507004, '算法练习用户', 'student,algorithm', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1998338632572506208, @user1_id, 2, '2003-04-04', '江苏省南京市', 'user1', NULL, '学生', 'Python,数据结构', '2024002', 1998338632572507001, '数据结构练习用户', 'student,python', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `gender` = VALUES(`gender`),
    `birthday` = VALUES(`birthday`),
    `address` = VALUES(`address`),
    `github` = VALUES(`github`),
    `company` = VALUES(`company`),
    `position` = VALUES(`position`),
    `skills` = VALUES(`skills`),
    `student_no` = VALUES(`student_no`),
    `school_id` = VALUES(`school_id`),
    `bio` = VALUES(`bio`),
    `tags` = VALUES(`tags`),
    `identity_status` = VALUES(`identity_status`),
    `updated_at` = CURRENT_TIMESTAMP;

DELETE FROM `class_user`
WHERE `id` NOT IN (
    1998338632572507301,
    1998338632572507302,
    1998338632572507303,
    1998338632572507304,
    1998338632572507305,
    1998338632572507306
);

INSERT INTO `class_user` (`id`, `class_id`, `user_id`, `join_type`, `join_status`, `join_at`, `reviewer_id`, `review_at`, `review_comment`)
VALUES
    (1998338632572507301, 1998338632572507201, @user_id, 'APPLY', 'APPROVED', CURRENT_TIMESTAMP, @admin1_id, CURRENT_TIMESTAMP, '审核通过'),
    (1998338632572507302, 1998338632572507203, @user_id, 'APPLY', 'APPROVED', CURRENT_TIMESTAMP, @admin1_id, CURRENT_TIMESTAMP, NULL),
    (1998338632572507303, 1998338632572507202, @user_id, 'APPLY', 'PENDING', NULL, NULL, NULL, NULL),
    (1998338632572507304, 1998338632572507201, @user1_id, 'INVITE', 'APPROVED', CURRENT_TIMESTAMP, @admin_id, CURRENT_TIMESTAMP, '邀请加入'),
    (1998338632572507305, 1998338632572507202, @user1_id, 'APPLY', 'REJECTED', NULL, @admin_id, CURRENT_TIMESTAMP, '班级已满'),
    (1998338632572507306, 1998338632572507204, @user1_id, 'APPLY', 'PENDING', NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
    `class_id` = VALUES(`class_id`),
    `user_id` = VALUES(`user_id`),
    `join_type` = VALUES(`join_type`),
    `join_status` = VALUES(`join_status`),
    `join_at` = VALUES(`join_at`),
    `reviewer_id` = VALUES(`reviewer_id`),
    `review_at` = VALUES(`review_at`),
    `review_comment` = VALUES(`review_comment`);
