-- 客户表：增加 local_id（同一租户内从 1 开始的连续序号，删除后可复用）
USE tenant_default;

ALTER TABLE contact ADD COLUMN local_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户内序号，从1连续可复用' AFTER tenant_id;
UPDATE contact c
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY create_time, id) AS rn
  FROM contact
) t ON c.id = t.id
SET c.local_id = t.rn;
ALTER TABLE contact ADD UNIQUE KEY uk_tenant_local_id (tenant_id, local_id);
