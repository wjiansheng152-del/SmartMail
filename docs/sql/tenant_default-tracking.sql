USE tenant_default;

CREATE TABLE IF NOT EXISTS tracking_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动id',
    delivery_id BIGINT UNSIGNED NOT NULL COMMENT '投递任务id',
    event_type VARCHAR(32) NOT NULL COMMENT 'open/click',
    link_url VARCHAR(500) DEFAULT NULL COMMENT '点击时链接',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_campaign_id (campaign_id),
    KEY idx_delivery_id (delivery_id),
    KEY idx_event_type (event_type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追踪事件';
