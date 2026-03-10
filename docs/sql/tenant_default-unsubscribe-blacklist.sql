USE tenant_default;

CREATE TABLE IF NOT EXISTS unsubscribe_list (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    email VARCHAR(255) NOT NULL COMMENT '邮箱',
    reason VARCHAR(128) DEFAULT NULL COMMENT '退订原因',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退订列表';

CREATE TABLE IF NOT EXISTS blacklist (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    email VARCHAR(255) NOT NULL COMMENT '邮箱',
    source VARCHAR(64) DEFAULT NULL COMMENT '来源',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单';
