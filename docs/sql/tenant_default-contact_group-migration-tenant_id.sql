-- 客户分组表按租户隔离
USE tenant_default;

ALTER TABLE contact_group ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识' AFTER id;
ALTER TABLE contact_group ADD KEY idx_tenant_id (tenant_id);
