package com.smartmail.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmail.delivery.config.PasswordCrypto;
import com.smartmail.delivery.entity.SmtpConfig;
import com.smartmail.delivery.mapper.SmtpConfigMapper;
import com.smartmail.delivery.web.dto.SmtpConfigDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户 SMTP 配置服务：按 user_id 查询/保存，密码加密存储、GET 脱敏。
 */
@Service
@RequiredArgsConstructor
public class SmtpConfigService {

    private final SmtpConfigMapper smtpConfigMapper;

    @Value("${app.smtp.encryption-key:}")
    private String encryptionKey;

    private static final String PASSWORD_PLACEHOLDER = "****";

    /**
     * 按用户 ID 查询配置，密码以占位返回。
     */
    public SmtpConfigDto getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SmtpConfig entity = smtpConfigMapper.selectOne(
                new LambdaQueryWrapper<SmtpConfig>().eq(SmtpConfig::getUserId, userId).last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return toDto(entity, true);
    }

    /**
     * 保存或更新当前用户的 SMTP 配置；若传入 password 则加密后存储。
     */
    public SmtpConfigDto save(Long userId, SmtpConfigDto dto) {
        if (userId == null || dto == null) {
            return null;
        }
        SmtpConfig existing = smtpConfigMapper.selectOne(
                new LambdaQueryWrapper<SmtpConfig>().eq(SmtpConfig::getUserId, userId).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        SmtpConfig entity = new SmtpConfig();
        entity.setUserId(userId);
        entity.setHost(dto.getHost() != null ? dto.getHost() : "");
        entity.setPort(dto.getPort() != null ? dto.getPort() : 25);
        entity.setUsername(dto.getUsername());
        entity.setFromEmail(dto.getFromEmail() != null ? dto.getFromEmail() : "");
        entity.setFromName(dto.getFromName());
        entity.setUseSsl(Boolean.TRUE.equals(dto.getUseSsl()));
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            String encrypted = PasswordCrypto.encrypt(dto.getPassword().trim(), encryptionKey);
            entity.setPasswordEncrypted(encrypted);
        }
        entity.setUpdateTime(now);
        if (existing != null) {
            entity.setId(existing.getId());
            if (entity.getPasswordEncrypted() == null) {
                entity.setPasswordEncrypted(existing.getPasswordEncrypted());
            }
            entity.setCreateTime(existing.getCreateTime());
            smtpConfigMapper.updateById(entity);
        } else {
            entity.setCreateTime(now);
            if (entity.getPasswordEncrypted() == null) {
                entity.setPasswordEncrypted("");
            }
            smtpConfigMapper.insert(entity);
        }
        return toDto(entity, true);
    }

    /**
     * 按用户 ID 查询配置实体（含解密后密码），供发送链路使用；无配置返回 null。
     */
    public SmtpConfig getEntityByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SmtpConfig entity = smtpConfigMapper.selectOne(
                new LambdaQueryWrapper<SmtpConfig>().eq(SmtpConfig::getUserId, userId).last("LIMIT 1"));
        return entity;
    }

    /**
     * 解密密码（供发送时使用）。
     */
    public String decryptPassword(String passwordEncrypted) {
        if (passwordEncrypted == null || passwordEncrypted.isEmpty()) {
            return "";
        }
        String decrypted = PasswordCrypto.decrypt(passwordEncrypted, encryptionKey);
        return decrypted != null ? decrypted : "";
    }

    private SmtpConfigDto toDto(SmtpConfig entity, boolean maskPassword) {
        SmtpConfigDto dto = new SmtpConfigDto();
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        dto.setPassword(maskPassword ? PASSWORD_PLACEHOLDER : null);
        dto.setFromEmail(entity.getFromEmail());
        dto.setFromName(entity.getFromName());
        dto.setUseSsl(Boolean.TRUE.equals(entity.getUseSsl()));
        return dto;
    }
}
