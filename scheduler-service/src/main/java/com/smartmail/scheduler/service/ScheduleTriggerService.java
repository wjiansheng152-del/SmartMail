package com.smartmail.scheduler.service;

import com.smartmail.common.tenant.TenantContext;
import com.smartmail.scheduler.entity.ScheduleJob;
import com.smartmail.scheduler.mapper.ScheduleJobMapper;
import com.smartmail.scheduler.mq.CampaignTriggerPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时扫描待执行计划并投递活动触发消息到 MQ，由 delivery 消费并生成发送任务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleTriggerService {

    private final ScheduleJobMapper scheduleJobMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.trigger.exchange:smartmail.trigger}")
    private String exchangeName;
    @Value("${app.trigger.routing-key:campaign.trigger}")
    private String routingKey;

    /** 默认租户，定时任务无请求上下文时使用 */
    private static final String DEFAULT_TENANT = "default";

    /**
     * 每分钟扫描一次：run_at 已到且 status=pending 的计划，投递触发消息并更新状态为 running。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    @Transactional(rollbackFor = Exception.class)
    public void triggerDueSchedules() {
        TenantContext.setTenantId(DEFAULT_TENANT);
        try {
            LocalDateTime now = LocalDateTime.now();
            List<ScheduleJob> due = scheduleJobMapper.selectPendingRunAt(now);
            // #region agent log
            try {
                String line = "{\"hypothesisId\":\"A\",\"message\":\"scheduler due jobs\",\"data\":{\"dueCount\":" + due.size() + ",\"now\":\"" + now + "\"},\"timestamp\":" + System.currentTimeMillis() + "}";
                Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) { /* ignore */ }
            // #endregion
            for (ScheduleJob job : due) {
                try {
                    CampaignTriggerPayload payload = new CampaignTriggerPayload(
                            job.getCampaignId(),
                            DEFAULT_TENANT,
                            job.getId()
                    );
                    rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
                    job.setStatus("running");
                    job.setUpdateTime(now);
                    scheduleJobMapper.updateById(job);
                    // #region agent log
                    try {
                        String line = "{\"hypothesisId\":\"A\",\"message\":\"triggered\",\"data\":{\"scheduleId\":" + job.getId() + ",\"campaignId\":" + job.getCampaignId() + "},\"timestamp\":" + System.currentTimeMillis() + "}";
                        Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (Exception e) { /* ignore */ }
                    // #endregion
                    log.info("Triggered schedule job id={}, campaignId={}", job.getId(), job.getCampaignId());
                } catch (Exception e) {
                    log.warn("Failed to trigger schedule job id={}, campaignId={}", job.getId(), job.getCampaignId(), e);
                    job.setStatus("failed");
                    job.setUpdateTime(now);
                    scheduleJobMapper.updateById(job);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }
}
