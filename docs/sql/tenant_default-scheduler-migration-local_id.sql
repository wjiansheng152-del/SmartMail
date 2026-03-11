-- 为 schedule_job 增加 local_id：按 created_by 从 1 连续，删除后可复用
USE tenant_default;

ALTER TABLE schedule_job
  ADD COLUMN local_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '创建人维度内序号，从1连续可复用' AFTER created_by;

UPDATE schedule_job s
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY created_by ORDER BY create_time, id) AS rn
  FROM schedule_job
) t ON s.id = t.id
SET s.local_id = t.rn;

ALTER TABLE schedule_job ADD UNIQUE KEY uk_created_by_local_id (created_by, local_id);
