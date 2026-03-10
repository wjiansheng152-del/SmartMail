package com.smartmail.delivery.channel;

/**
 * 统一邮件发送接口，SMTP 与第三方 API 均实现此接口。
 */
public interface EmailSender {

    SendResult send(SendRequest request);

    String channelType();
}
