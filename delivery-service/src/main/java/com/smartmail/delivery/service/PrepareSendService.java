package com.smartmail.delivery.service;

import com.smartmail.common.tenant.TenantContext;
import com.smartmail.delivery.client.DownstreamClient;
import com.smartmail.delivery.entity.CampaignBatch;
import com.smartmail.delivery.entity.DeliveryTask;
import com.smartmail.delivery.mapper.CampaignBatchMapper;
import com.smartmail.delivery.mapper.DeliveryTaskMapper;
import com.smartmail.delivery.mq.SendTaskPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 根据活动触发消息拉取活动/模板/联系人，过滤退订与黑名单，创建批次与投递任务并投递到发送队列。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrepareSendService {

    private final DownstreamClient downstreamClient;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final CampaignBatchMapper campaignBatchMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.send.exchange:smartmail.send}")
    private String sendExchange;
    @Value("${app.send.routing-key:send.task}")
    private String sendRoutingKey;
    @Value("${app.default.from:noreply@smartmail.local}")
    private String defaultFrom;

    /**
     * 处理活动触发：拉取活动、模板、分组联系人，过滤后创建批次与投递任务并投递 MQ。
     * campaignId 为活动 local_id，createdBy 非空时请求 campaign 会带 X-User-Id 以按 local_id 查询。
     */
    @Transactional(rollbackFor = Exception.class)
    public void prepareAndEnqueue(Long campaignId, Long createdBy, String tenantId, Long scheduleId) {
        TenantContext.setTenantId(tenantId != null ? tenantId : "default");
        try {
            Map<String, Object> campaign = downstreamClient.getCampaign(campaignId, tenantId, createdBy);
            boolean campaignNull = (campaign == null || campaign.get("data") == null);
            // #region agent log
            try {
                String line = "{\"hypothesisId\":\"C\",\"message\":\"after getCampaign\",\"data\":{\"campaignId\":" + campaignId + ",\"campaignNull\":" + campaignNull + "},\"timestamp\":" + System.currentTimeMillis() + "}";
                Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) { /* ignore */ }
            // #endregion
            if (campaignNull) {
                log.warn("Campaign not found: campaignId={}", campaignId);
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) campaign.get("data");
            Long campaignCreatedBy = longFrom(c.get("createdBy"));
            Object templateIdObj = c.get("templateId");
            Object groupIdObj = c.get("groupId");
            if (templateIdObj == null || groupIdObj == null) {
                log.warn("Campaign missing templateId or groupId: campaignId={}", campaignId);
                return;
            }
            Long templateId = longFrom(templateIdObj);
            Long groupId = longFrom(groupIdObj);

            Map<String, Object> templateRes = downstreamClient.getTemplate(templateId, tenantId);
            boolean templateNull = (templateRes == null || templateRes.get("data") == null);
            // #region agent log
            try {
                String line = "{\"hypothesisId\":\"C\",\"message\":\"after getTemplate\",\"data\":{\"campaignId\":" + campaignId + ",\"templateId\":" + templateId + ",\"templateNull\":" + templateNull + "},\"timestamp\":" + System.currentTimeMillis() + "}";
                Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) { /* ignore */ }
            // #endregion
            if (templateNull) {
                log.warn("Template not found: templateId={}", templateId);
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> template = (Map<String, Object>) templateRes.get("data");
            String subject = (String) template.get("subject");
            String bodyHtml = (String) template.get("bodyHtml");
            if (subject == null) {
                subject = "";
            }
            if (bodyHtml == null) {
                bodyHtml = "";
            }

            List<Map<String, Object>> contacts = downstreamClient.getContactsByGroup(groupId, tenantId);
            Set<String> blacklist = downstreamClient.getBlacklistEmails(tenantId).stream().collect(Collectors.toSet());
            Set<String> unsubscribed = downstreamClient.getUnsubscribeEmails(tenantId).stream().collect(Collectors.toSet());
            // #region agent log
            try {
                String line = "{\"hypothesisId\":\"D\",\"message\":\"after getContactsByGroup\",\"data\":{\"campaignId\":" + campaignId + ",\"groupId\":" + groupId + ",\"contactsSize\":" + (contacts != null ? contacts.size() : -1) + "},\"timestamp\":" + System.currentTimeMillis() + "}";
                Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) { /* ignore */ }
            // #endregion

            List<Map<String, Object>> toSend = contacts.stream()
                    .filter(m -> {
                        String email = (String) m.get("email");
                        return email != null && !blacklist.contains(email) && !unsubscribed.contains(email);
                    })
                    .toList();

            // #region agent log
            try {
                String line = "{\"hypothesisId\":\"E\",\"message\":\"after filter\",\"data\":{\"campaignId\":" + campaignId + ",\"toSendSize\":" + toSend.size() + "},\"timestamp\":" + System.currentTimeMillis() + "}";
                Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) { /* ignore */ }
            // #endregion
            if (toSend.isEmpty()) {
                log.info("No contacts to send after filter: campaignId={}", campaignId);
                return;
            }

            String batchNo = "batch-" + campaignId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();
            CampaignBatch batch = new CampaignBatch();
            batch.setCampaignId(campaignId);
            batch.setBatchNo(batchNo);
            batch.setTotalCount(toSend.size());
            batch.setSuccessCount(0);
            batch.setFailCount(0);
            batch.setCreateTime(now);
            batch.setUpdateTime(now);
            campaignBatchMapper.insert(batch);
            Long batchId = batch.getId();

            String trackingBaseUrl = downstreamClient.getTrackingBaseUrl().replaceFirst("/$", "");

            for (Map<String, Object> contact : toSend) {
                String email = (String) contact.get("email");
                Long contactId = longFrom(contact.get("id"));
                DeliveryTask task = new DeliveryTask();
                task.setCampaignId(campaignId);
                task.setBatchId(batchId);
                task.setContactId(contactId);
                task.setEmail(email);
                task.setStatus("pending");
                task.setCreateTime(now);
                task.setUpdateTime(now);
                deliveryTaskMapper.insert(task);
                Long deliveryId = task.getId();

                String htmlWithPixel = bodyHtml + "<img src=\"" + trackingBaseUrl + "/api/tracking/pixel/" + deliveryId + "?campaignId=" + campaignId + "\" width=\"1\" height=\"1\" alt=\"\" />";
                SendTaskPayload payload = new SendTaskPayload();
                payload.setDeliveryId(deliveryId);
                payload.setCampaignId(campaignId);
                payload.setBatchId(batchId);
                payload.setContactId(contactId);
                payload.setTo(email);
                payload.setSubject(subject);
                payload.setHtmlBody(htmlWithPixel);
                payload.setFrom(defaultFrom);
                payload.setChannel("smtp");
                payload.setTenantId(tenantId);
                payload.setSmtpConfigUserId(campaignCreatedBy);
                rabbitTemplate.convertAndSend(sendExchange, sendRoutingKey, payload);
            }
            log.info("Enqueued {} send tasks for campaignId={}, batchId={}", toSend.size(), campaignId, batchId);
        } finally {
            TenantContext.clear();
        }
    }

    private static Long longFrom(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }
}
