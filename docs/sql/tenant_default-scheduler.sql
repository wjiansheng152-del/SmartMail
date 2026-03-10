USE tenant_default;

CREATE TABLE IF NOT EXISTS schedule_job (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动id',
    cron_expr VARCHAR(128) DEFAULT NULL COMMENT 'cron表达式',
    run_at DATETIME DEFAULT NULL COMMENT '单次执行时间',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/done/failed',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_campaign_id (campaign_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务';
