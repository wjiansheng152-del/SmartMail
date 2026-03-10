-- 若已存在 delivery_task 表且无 batch_id 列，可执行本迁移（与 tenant_default-delivery.sql 中表结构对齐）
USE tenant_default;

ALTER TABLE delivery_task
ADD COLUMN batch_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '批次id' AFTER campaign_id;

ALTER TABLE delivery_task
ADD KEY idx_batch_id (batch_id);

-- 注意：若表中已有数据，DEFAULT 0 可能导致与 campaign_batch 无法关联，建议在无数据或测试环境执行；生产请先备份并视情况回填 batch_id。
