-- 客户、标签、分组表（在 tenant_default 或各租户 schema 下执行）
USE tenant_default;

CREATE TABLE IF NOT EXISTS contact (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    email VARCHAR(255) NOT NULL COMMENT '邮箱',
    name VARCHAR(128) DEFAULT NULL COMMENT '姓名',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户';

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '标签名',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签';

CREATE TABLE IF NOT EXISTS contact_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(128) NOT NULL COMMENT '分组名',
    rule_type VARCHAR(32) NOT NULL DEFAULT 'static' COMMENT 'static-静态/dynamic-动态',
    rule_expr VARCHAR(500) DEFAULT NULL COMMENT '动态规则表达式',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户分组';

CREATE TABLE IF NOT EXISTS contact_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    contact_id BIGINT UNSIGNED NOT NULL COMMENT '客户id',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签id',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contact_tag (contact_id, tag_id),
    KEY idx_contact_id (contact_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户-标签关联';

CREATE TABLE IF NOT EXISTS contact_group_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '分组id',
    contact_id BIGINT UNSIGNED NOT NULL COMMENT '客户id',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_contact (group_id, contact_id),
    KEY idx_group_id (group_id),
    KEY idx_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分组-客户关联(静态分组)';
