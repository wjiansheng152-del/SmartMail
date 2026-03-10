package com.smartmail.scheduler.service;

import com.smartmail.scheduler.entity.ScheduleJob;
import com.smartmail.scheduler.mapper.ScheduleJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 发送计划业务：创建计划并落库、查询当前租户计划列表。
 */
@Service
@RequiredArgsConstructor
public class ScheduleJobService {

    private static final DateTimeFormatter RUN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScheduleJobMapper scheduleJobMapper;

    /**
     * 创建一条发送计划。runAt 格式为 yyyy-MM-dd HH:mm:ss，与 cronExpr 二选一，优先 runAt。
     */
    public Long create(Long campaignId, String cronExpr, String runAt) {
        ScheduleJob job = new ScheduleJob();
        job.setCampaignId(campaignId);
        job.setCronExpr(cronExpr);
        if (runAt != null && !runAt.isBlank()) {
            job.setRunAt(LocalDateTime.parse(runAt.trim(), RUN_AT_FORMAT));
        }
        job.setStatus("pending");
        LocalDateTime now = LocalDateTime.now();
        job.setCreateTime(now);
        job.setUpdateTime(now);
        scheduleJobMapper.insert(job);
        return job.getId();
    }

    /** 查询当前租户下全部计划列表（按创建时间倒序） */
    public List<ScheduleJob> list() {
        return scheduleJobMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScheduleJob>()
                        .orderByDesc(ScheduleJob::getCreateTime)
        );
    }
}
