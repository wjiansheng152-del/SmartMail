-- 默认租户 schema（示例：tenant_default）
-- 各业务表按建表规约：id, create_time, update_time，小写字母/数字
CREATE DATABASE IF NOT EXISTS tenant_default DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tenant_default;

-- 后续各服务 DDL 将按模块在此 schema 下建表，或各服务独立建库 tenant_xxx
