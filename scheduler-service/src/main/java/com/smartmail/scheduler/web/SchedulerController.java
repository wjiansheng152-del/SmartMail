package com.smartmail.scheduler.web;

import com.smartmail.common.result.Result;
import com.smartmail.scheduler.entity.ScheduleJob;
import com.smartmail.scheduler.service.ScheduleJobService;
import lombok.RequiredArgsConstructor;
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

    /** 创建一条发送计划，返回计划 ID；request 含 campaignId、cronExpr、runAt 等 */
    @PostMapping
    public Result<Long> create(@RequestBody ScheduleCreateRequest request) {
        Long id = scheduleJobService.create(request.getCampaignId(), request.getCronExpr(), request.getRunAt());
        return Result.ok(id);
    }

    /** 查询当前租户下的发送计划列表 */
    @GetMapping("/list")
    public Result<List<ScheduleJob>> list() {
        return Result.ok(scheduleJobService.list());
    }

    /** 创建计划请求体：活动 ID、Cron 表达式、或一次性执行时间 runAt */
    public static class ScheduleCreateRequest {
        private Long campaignId;
        private String cronExpr;
        private String runAt;

        public Long getCampaignId() { return campaignId; }
        public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
        public String getCronExpr() { return cronExpr; }
        public void setCronExpr(String cronExpr) { this.cronExpr = cronExpr; }
        public String getRunAt() { return runAt; }
        public void setRunAt(String runAt) { this.runAt = runAt; }
    }
}
