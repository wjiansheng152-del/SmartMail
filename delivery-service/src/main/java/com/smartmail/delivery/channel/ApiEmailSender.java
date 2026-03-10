package com.smartmail.delivery.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 第三方邮件 API 发送适配（示例：可替换为 SendGrid/SES 等具体实现）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.channel.api.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ApiEmailSender implements EmailSender {

    private final RestTemplate restTemplate;

    @Override
    public SendResult send(SendRequest request) {
        String apiUrl = System.getProperty("app.channel.api.url", "https://api.example.com/send");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
                    "to", request.getTo(),
                    "subject", request.getSubject(),
                    "html", request.getHtmlBody(),
                    "from", request.getFrom() != null ? request.getFrom() : "noreply@smartmail.local"
            ), headers);
            ResponseEntity<String> resp = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return SendResult.builder().success(true).messageId(resp.getBody()).build();
            }
            return SendResult.builder().success(false).errorMessage(resp.getBody()).build();
        } catch (Exception e) {
            log.warn("API send failed: {}", e.getMessage());
            return SendResult.builder().success(false).errorMessage(e.getMessage()).build();
        }
    }

    @Override
    public String channelType() {
        return "api";
    }
}
