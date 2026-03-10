package com.smartmail.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 下游服务 base URL 配置，用于拉取活动、模板、联系人等。
 * 可指向网关或直连各服务。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.downstream")
public class DownstreamServicesProperties {

    /** 网关或 contact 服务地址，如 http://localhost:8080 或 http://localhost:8082 */
    private String contactBaseUrl = "http://localhost:8082";
    /** 模板服务地址 */
    private String templateBaseUrl = "http://localhost:8083";
    /** 活动服务地址 */
    private String campaignBaseUrl = "http://localhost:8084";
    /** 追踪像素/点击链接的前缀，邮件内嵌用，如 http://localhost:8080 */
    private String trackingBaseUrl = "http://localhost:8080";
}
