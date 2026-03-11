package com.smartmail.delivery.channel;

import com.smartmail.delivery.entity.SmtpConfig;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 根据用户 SMTP 配置构建 JavaMailSender，供发送链路按「活动创建人」配置发信使用。
 */
public final class UserSmtpMailSenderFactory {

    private UserSmtpMailSenderFactory() {
    }

    /**
     * 根据配置与明文密码构建 JavaMailSender。
     *
     * @param config        用户 smtp_config 实体
     * @param plainPassword 解密后的密码，可为空串
     * @return 配置好的 JavaMailSender，不会为 null
     */
    public static JavaMailSender build(SmtpConfig config, String plainPassword) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(config.getHost() != null ? config.getHost() : "localhost");
        impl.setPort(config.getPort() != null ? config.getPort() : 25);
        impl.setUsername(config.getUsername());
        impl.setPassword(plainPassword != null ? plainPassword : "");
        impl.setProtocol("smtp");
        Properties props = new Properties();
        if (Boolean.TRUE.equals(config.getUseSsl())) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        props.put("mail.smtp.auth", "true");
        impl.setJavaMailProperties(props);
        return impl;
    }
}
