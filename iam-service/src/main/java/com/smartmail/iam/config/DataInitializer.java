package com.smartmail.iam.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartmail.iam.entity.TenantMetadata;
import com.smartmail.iam.entity.User;
import com.smartmail.iam.repository.TenantMetadataRepository;
import com.smartmail.iam.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 启动时初始化默认租户（default → tenant_default）与默认用户（admin / admin123），一租户一账号。
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantMetadataRepository tenantMetadataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        if (tenantMetadataRepository.findByTenantId("default").isEmpty()) {
            TenantMetadata defaultTenant = new TenantMetadata();
            defaultTenant.setTenantId("default");
            defaultTenant.setSchemaName("tenant_default");
            defaultTenant.setCreateTime(now);
            defaultTenant.setUpdateTime(now);
            tenantMetadataRepository.save(defaultTenant);
        }
        if (userRepository.findByUsername("admin").isEmpty()) {
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setTenantId("default");
            user.setCreateTime(now);
            user.setUpdateTime(now);
            userRepository.save(user);
        }
    }
}
