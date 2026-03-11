USE tenant_default;

ALTER TABLE email_template ADD COLUMN local_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户内序号，从1连续可复用' AFTER tenant_id;
UPDATE email_template c
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY create_time, id) AS rn
  FROM email_template
) t ON c.id = t.id
SET c.local_id = t.rn;
ALTER TABLE email_template ADD UNIQUE KEY uk_tenant_local_id (tenant_id, local_id);
