USE tenant_default;

ALTER TABLE campaign ADD COLUMN local_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户内序号，从1连续可复用' AFTER id;
UPDATE campaign c
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY created_by ORDER BY create_time, id) AS rn
  FROM campaign
) t ON c.id = t.id
SET c.local_id = t.rn;
ALTER TABLE campaign ADD UNIQUE KEY uk_created_by_local_id (created_by, local_id);
