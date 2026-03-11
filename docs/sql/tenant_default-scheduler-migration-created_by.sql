-- 为 schedule_job 增加 created_by，用于投递时按「创建人 + local_id」查询活动
USE tenant_default;

ALTER TABLE schedule_job
  ADD COLUMN created_by BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '活动创建人用户ID，用于 GET /campaign/{local_id} 时传 X-User-Id' AFTER campaign_id;
