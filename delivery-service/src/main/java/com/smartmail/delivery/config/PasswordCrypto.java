package com.smartmail.delivery.config;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SMTP 密码加密/解密工具，使用 AES-128-CBC。
 * 密钥通过 app.smtp.encryption-key 配置（建议 16 字节或 Base64 编码的 16 字节）。
 */
@Slf4j
public final class PasswordCrypto {

    private static final String ALG = "AES";
    private static final String TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final int IV_LEN = 16;

    /**
     * 加密：plaintext 为明文密码，key 为配置的密钥（UTF-8 或 Base64 解码后取前 16 字节）。
     */
    public static String encrypt(String plaintext, String key) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        if (key == null || key.isEmpty()) {
            log.warn("Encryption key not set, storing password in plaintext is not recommended");
            return plaintext;
        }
        try {
            byte[] keyBytes = ensureKeyBytes(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALG);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            byte[] iv = new byte[IV_LEN];
            new java.security.SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Password encrypt failed", e);
            return null;
        }
    }

    /**
     * 解密：ciphertext 为加密后的 Base64 字符串。
     */
    public static String decrypt(String ciphertext, String key) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }
        if (key == null || key.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= IV_LEN) {
                return null;
            }
            byte[] keyBytes = ensureKeyBytes(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALG);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            byte[] encrypted = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, IV_LEN, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Password decrypt failed", e);
            return null;
        }
    }

    private static byte[] ensureKeyBytes(String key) {
        byte[] keyBytes;
        if (key.length() == 16 && !key.contains("=")) {
            keyBytes = key.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                keyBytes = Base64.getDecoder().decode(key);
            } catch (IllegalArgumentException e) {
                keyBytes = key.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (keyBytes.length < 16) {
            byte[] padded = new byte[16];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return padded;
        }
        if (keyBytes.length > 16) {
            byte[] trimmed = new byte[16];
            System.arraycopy(keyBytes, 0, trimmed, 0, 16);
            return trimmed;
        }
        return keyBytes;
    }

    private PasswordCrypto() {
    }
}
