USE tenant_default;

CREATE TABLE IF NOT EXISTS delivery_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动id',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '批次id',
    contact_id BIGINT UNSIGNED NOT NULL COMMENT '客户id',
    email VARCHAR(255) NOT NULL COMMENT '收件邮箱',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/sent/failed',
    channel VARCHAR(32) DEFAULT NULL COMMENT 'smtp/api',
    fail_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_campaign_id (campaign_id),
    KEY idx_batch_id (batch_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递任务';
