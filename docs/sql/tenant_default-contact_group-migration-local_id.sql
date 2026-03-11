USE tenant_default;

ALTER TABLE contact_group ADD COLUMN local_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户内序号，从1连续可复用' AFTER tenant_id;
UPDATE contact_group c
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY create_time, id) AS rn
  FROM contact_group
) t ON c.id = t.id
SET c.local_id = t.rn;
ALTER TABLE contact_group ADD UNIQUE KEY uk_tenant_local_id (tenant_id, local_id);
