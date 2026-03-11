-- 邮件模板表按租户隔离
USE tenant_default;

ALTER TABLE email_template ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识' AFTER id;
ALTER TABLE email_template ADD KEY idx_tenant_id (tenant_id);
