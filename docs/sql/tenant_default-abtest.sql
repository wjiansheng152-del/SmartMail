USE tenant_default;

CREATE TABLE IF NOT EXISTS campaign_ab_assignment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动id',
    contact_id BIGINT UNSIGNED NOT NULL COMMENT '客户id',
    variant VARCHAR(8) NOT NULL COMMENT 'A/B',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_campaign_contact (campaign_id, contact_id),
    KEY idx_campaign_id (campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B测试分流记录';
