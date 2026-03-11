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
 * 租户元数据实体，与 platform 库 tenant_metadata 表一致。
 * <p>
 * 用于注册时创建新租户；tenant_id 唯一，schema_name 指向共享库时均为 tenant_default。
 * </p>
 */
@Getter
@Setter
@Entity
@Table(name = "tenant_metadata")
public class TenantMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 租户标识，唯一 */
    @Column(name = "tenant_id", nullable = false, unique = true, length = 64)
    private String tenantId;

    /** 对应 MySQL schema(database) 名，共享库场景下均为 tenant_default */
    @Column(name = "schema_name", nullable = false, length = 64)
    private String schemaName;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
