-- 用户 SMTP 配置表（租户库）：按用户存储，发送时使用活动创建人的配置
USE tenant_default;

CREATE TABLE IF NOT EXISTS smtp_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户id，对应 platform.sys_user.id',
    host VARCHAR(255) NOT NULL COMMENT 'SMTP 主机',
    port INT UNSIGNED NOT NULL DEFAULT 25 COMMENT 'SMTP 端口',
    username VARCHAR(128) DEFAULT NULL COMMENT '认证用户名',
    password_encrypted VARCHAR(512) DEFAULT NULL COMMENT '认证密码（加密存储）',
    from_email VARCHAR(255) NOT NULL COMMENT '发件人邮箱',
    from_name VARCHAR(128) DEFAULT NULL COMMENT '发件人显示名',
    use_ssl TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用 SSL：0-否 1-是',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 SMTP 配置';
