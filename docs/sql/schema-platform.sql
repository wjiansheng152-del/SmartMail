-- 平台库：租户元数据、全局配置等（与 IAM 用户表可同库或同实例不同 schema）
-- MySQL 中 schema 即 database，库名小写
CREATE DATABASE IF NOT EXISTS platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE platform;

-- 租户元数据表：业务上具有唯一特性的 tenant_id 建唯一索引
CREATE TABLE IF NOT EXISTS tenant_metadata (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识',
    schema_name VARCHAR(64) NOT NULL COMMENT '对应 MySQL schema(database) 名',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id),
    KEY idx_schema_name (schema_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户元数据';
