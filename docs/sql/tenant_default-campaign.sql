USE tenant_default;

CREATE TABLE IF NOT EXISTS campaign (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(128) NOT NULL COMMENT '活动名称',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '模板id',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '分组id',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/scheduled/sending/done',
    ab_config VARCHAR(1000) DEFAULT NULL COMMENT 'A/B测试配置JSON',
    scheduled_at DATETIME DEFAULT NULL COMMENT '计划发送时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动';

CREATE TABLE IF NOT EXISTS campaign_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动id',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    total_count INT UNSIGNED NOT NULL DEFAULT 0,
    success_count INT UNSIGNED NOT NULL DEFAULT 0,
    fail_count INT UNSIGNED NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_campaign_id (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发送批次';
