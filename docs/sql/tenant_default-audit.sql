USE tenant_default;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户id',
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource VARCHAR(128) DEFAULT NULL COMMENT '资源类型',
    resource_id VARCHAR(64) DEFAULT NULL COMMENT '资源id',
    detail VARCHAR(500) DEFAULT NULL COMMENT '详情',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';
