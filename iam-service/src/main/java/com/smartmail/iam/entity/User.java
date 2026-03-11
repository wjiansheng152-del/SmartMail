package com.smartmail.iam.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体，存放于 platform 库/ schema。
 * <p>
 * 一租户一账号：tenant_id 唯一，保证每个租户仅有一条用户记录。
 * </p>
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    /** 租户 ID，唯一约束实现一租户一账号 */
    @Column(name = "tenant_id", length = 64, unique = true)
    private String tenantId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
