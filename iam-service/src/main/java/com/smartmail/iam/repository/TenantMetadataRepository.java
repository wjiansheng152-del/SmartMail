package com.smartmail.iam.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartmail.iam.entity.TenantMetadata;

/**
 * 租户元数据仓储，用于注册时写入新租户、初始化时检查默认租户。
 */
public interface TenantMetadataRepository extends JpaRepository<TenantMetadata, Long> {

    Optional<TenantMetadata> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}
