package com.smartmail.delivery.channel;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 按租户或默认配置选择发送通道（smtp / api）。
 */
@Component
@RequiredArgsConstructor
public class SendStrategy {

    private final List<EmailSender> senders;

    public EmailSender select(String tenantId, String preferredChannel) {
        if (preferredChannel != null && !preferredChannel.isBlank()) {
            return senders.stream()
                    .filter(s -> preferredChannel.equals(s.channelType()))
                    .findFirst()
                    .orElseGet(() -> senders.isEmpty() ? null : senders.get(0));
        }
        return senders.isEmpty() ? null : senders.get(0);
    }
}
