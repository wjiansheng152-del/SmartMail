package com.smartmail.scheduler.web;

import com.smartmail.common.result.Result;
import com.smartmail.scheduler.entity.ScheduleJob;
import com.smartmail.scheduler.service.ScheduleJobService;
import com.smartmail.scheduler.web.dto.ScheduleJobListItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务编排：创建发送计划（关联活动、cron 或一次性 runAt），查询计划列表。
 * <p>
 * 计划持久化到 schedule_job 表，到点后由 ScheduleTriggerService 投递 CampaignTriggerPayload 到 MQ，由 delivery 消费并生成发送任务。
 * </p>
 */
@RestController
@RequestMapping("/api/scheduler/schedule")
@RequiredArgsConstructor
public class SchedulerController {

    private final ScheduleJobService scheduleJobService;

    /** 创建一条发送计划，返回计划 ID（local_id，按用户从 1 连续）；request 含 campaignId、createdBy、cronExpr、runAt 等 */
    @PostMapping
    public Result<Long> create(@RequestBody ScheduleCreateRequest request) {
        Long localId = scheduleJobService.create(request.getCampaignId(), request.getCreatedBy(), request.getCronExpr(), request.getRunAt());
        return Result.ok(localId);
    }

    /** 查询发送计划列表；若带 X-User-Id 则仅返回该用户的计划；每项 id 为 local_id（按用户从 1 连续） */
    @GetMapping("/list")
    public Result<List<ScheduleJobListItem>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long createdBy = null;
        if (userIdStr != null && !userIdStr.isBlank()) {
            try {
                createdBy = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        List<ScheduleJob> jobs = scheduleJobService.listByCreatedBy(createdBy);
        List<ScheduleJobListItem> items = jobs.stream()
                .map(job -> {
                    ScheduleJobListItem item = new ScheduleJobListItem();
                    BeanUtils.copyProperties(job, item);
                    item.setId(job.getLocalId() != null ? job.getLocalId().longValue() : job.getId());
                    return item;
                })
                .toList();
        return Result.ok(items);
    }

    /** 创建计划请求体：活动 ID（local_id）、活动创建人 userId、Cron 或 runAt */
    public static class ScheduleCreateRequest {
        private Long campaignId;
        private Long createdBy;
        private String cronExpr;
        private String runAt;

        public Long getCampaignId() { return campaignId; }
        public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
        public String getCronExpr() { return cronExpr; }
        public void setCronExpr(String cronExpr) { this.cronExpr = cronExpr; }
        public String getRunAt() { return runAt; }
        public void setRunAt(String runAt) { this.runAt = runAt; }
    }
}
