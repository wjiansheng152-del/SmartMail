-- 为 campaign 表增加 created_by 列，表示创建该活动的用户 ID（用于发送时选用该用户的 SMTP）
USE tenant_default;

ALTER TABLE campaign
ADD COLUMN created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人用户id' AFTER group_id;

ALTER TABLE campaign
ADD KEY idx_created_by (created_by);
