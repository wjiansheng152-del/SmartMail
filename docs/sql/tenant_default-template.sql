USE tenant_default;

CREATE TABLE IF NOT EXISTS email_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    subject VARCHAR(255) NOT NULL COMMENT '邮件主题',
    body_html TEXT NOT NULL COMMENT 'HTML正文',
    variables VARCHAR(500) DEFAULT NULL COMMENT '变量占位符逗号分隔如: name,company',
    version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '版本号',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件模板';
