package com.smartmail.delivery.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmail.delivery.config.DownstreamServicesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 调用 contact、template、campaign 服务获取活动、模板、联系人及退订/黑名单列表。
 * 请求头携带 X-Tenant-Id，供下游租户路由。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DownstreamClient {

    private final RestTemplate restTemplate;
    private final DownstreamServicesProperties props;
    private static final String HEADER_TENANT = "X-Tenant-Id";

    /** 获取活动详情。createdBy 非空时带 X-User-Id，campaign 服务按 local_id 查询；否则按内部主键查询。 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCampaign(Long campaignId, String tenantId, Long createdBy) {
        String url = props.getCampaignBaseUrl() + "/api/campaign/campaign/" + campaignId;
        Map<String, Object> result = getWithTenantAndUser(url, tenantId, createdBy, new ParameterizedTypeReference<Map<String, Object>>() {});
        // #region agent log
        try {
            String esc = url.replace("\"", "\\\"");
            String line = "{\"hypothesisId\":\"C\",\"message\":\"getCampaign result\",\"data\":{\"url\":\"" + esc + "\",\"resultNull\":" + (result == null) + "},\"timestamp\":" + System.currentTimeMillis() + "}";
            Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { /* ignore */ }
        // #endregion
        return result;
    }

    /** 获取模板详情 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTemplate(Long templateId, String tenantId) {
        String url = props.getTemplateBaseUrl() + "/api/template/template/" + templateId;
        return getWithTenant(url, tenantId, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /** 获取分组下所有联系人（分页拉取直至取完） */
    public List<Map<String, Object>> getContactsByGroup(Long groupId, String tenantId) {
        List<Map<String, Object>> all = new ArrayList<>();
        int page = 1;
        int size = 500;
        while (true) {
            String url = props.getContactBaseUrl() + "/api/contact/contact/page?page=" + page + "&size=" + size + "&groupId=" + groupId;
            Map<String, Object> pageBody = getWithTenant(url, tenantId, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (pageBody == null) {
                break;
            }
            Object data = pageBody.get("data");
            if (data == null) {
                break;
            }
            Map<String, Object> pageData = (Map<String, Object>) data;
            Object records = pageData.get("records");
            if (records == null || !(records instanceof List)) {
                break;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) records;
            if (list.isEmpty()) {
                break;
            }
            all.addAll(list);
            if (list.size() < size) {
                break;
            }
            page++;
        }
        return all;
    }

    /** 获取黑名单邮箱列表 */
    @SuppressWarnings("unchecked")
    public List<String> getBlacklistEmails(String tenantId) {
        String url = props.getContactBaseUrl() + "/api/contact/blacklist/list";
        Map<String, Object> body = getWithTenant(url, tenantId, new ParameterizedTypeReference<Map<String, Object>>() {});
        if (body == null || body.get("data") == null) {
            return List.of();
        }
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("data");
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .map(m -> (String) m.get("email"))
                .filter(e -> e != null)
                .toList();
    }

    /** 获取已退订邮箱列表 */
    public List<String> getUnsubscribeEmails(String tenantId) {
        String url = props.getContactBaseUrl() + "/api/contact/unsubscribe/list";
        Map<String, Object> body = getWithTenant(url, tenantId, new ParameterizedTypeReference<Map<String, Object>>() {});
        if (body == null || body.get("data") == null) {
            return List.of();
        }
        List<String> list = (List<String>) body.get("data");
        return list != null ? list : List.of();
    }

    public String getTrackingBaseUrl() {
        return props.getTrackingBaseUrl();
    }

    private <T> T getWithTenant(String url, String tenantId, ParameterizedTypeReference<T> typeRef) {
        return getWithTenantAndUser(url, tenantId, null, typeRef);
    }

    private <T> T getWithTenantAndUser(String url, String tenantId, Long userId, ParameterizedTypeReference<T> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TENANT, tenantId != null ? tenantId : "default");
        if (userId != null) {
            headers.set("X-User-Id", String.valueOf(userId));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<T> res = restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);
            return res.getBody();
        } catch (Exception e) {
            log.warn("Downstream call failed: {} {}", url, e.getMessage());
            return null;
        }
    }
}
