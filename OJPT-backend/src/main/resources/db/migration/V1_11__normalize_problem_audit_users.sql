SET @admin_id := 1998338632572506113;
SET @admin1_id := 1998338632572506114;
SET @user_id := 1998338632572506117;
SET @user1_id := 1998338632572506121;

UPDATE `problem`
SET `created_by` = @admin_id
WHERE `created_by` IS NOT NULL
  AND `created_by` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);

UPDATE `problem`
SET `updated_by` = @admin_id
WHERE `updated_by` IS NOT NULL
  AND `updated_by` NOT IN (@admin_id, @admin1_id, @user_id, @user1_id);
