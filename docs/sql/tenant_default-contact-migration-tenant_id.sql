-- 客户表按租户隔离：增加 tenant_id，邮箱唯一性改为「租户+邮箱」
USE tenant_default;

-- 增加租户列，已有数据归为 default
ALTER TABLE contact ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户标识' AFTER id;
ALTER TABLE contact ADD KEY idx_tenant_id (tenant_id);
-- 邮箱改为按租户唯一
ALTER TABLE contact DROP INDEX uk_email;
ALTER TABLE contact ADD UNIQUE KEY uk_tenant_email (tenant_id, email);
