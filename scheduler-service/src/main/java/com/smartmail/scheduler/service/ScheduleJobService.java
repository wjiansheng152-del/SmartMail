package com.smartmail.scheduler.service;

import com.smartmail.scheduler.entity.ScheduleJob;
import com.smartmail.scheduler.mapper.ScheduleJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 发送计划业务：创建计划并落库（分配按用户维度的 local_id）、查询计划列表。
 */
@Service
@RequiredArgsConstructor
public class ScheduleJobService {

    private static final DateTimeFormatter RUN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScheduleJobMapper scheduleJobMapper;

    /**
     * 创建一条发送计划。campaignId 为活动 local_id，createdBy 为活动创建人；分配 local_id（按用户从 1 连续可复用）。
     * runAt 格式为 yyyy-MM-dd HH:mm:ss，与 cronExpr 二选一，优先 runAt。
     *
     * @return 新计划的 local_id（对外展示的计划 ID），非内部主键
     */
    public Long create(Long campaignId, Long createdBy, String cronExpr, String runAt) {
        ScheduleJob job = new ScheduleJob();
        job.setCampaignId(campaignId);
        job.setCreatedBy(createdBy);
        if (createdBy != null) {
            job.setLocalId(nextLocalIdForCreatedBy(createdBy));
        } else {
            job.setLocalId(1);
        }
        job.setCronExpr(cronExpr);
        if (runAt != null && !runAt.isBlank()) {
            job.setRunAt(LocalDateTime.parse(runAt.trim(), RUN_AT_FORMAT));
        }
        job.setStatus("pending");
        LocalDateTime now = LocalDateTime.now();
        job.setCreateTime(now);
        job.setUpdateTime(now);
        scheduleJobMapper.insert(job);
        return job.getLocalId() != null ? job.getLocalId().longValue() : job.getId();
    }

    /** 为指定创建人分配下一个可复用的 local_id（最小未用正整数） */
    public int nextLocalIdForCreatedBy(Long createdBy) {
        if (createdBy == null) return 1;
        List<ScheduleJob> list = scheduleJobMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScheduleJob>()
                        .eq(ScheduleJob::getCreatedBy, createdBy)
                        .orderByAsc(ScheduleJob::getLocalId));
        int next = 1;
        for (ScheduleJob j : list) {
            Integer lid = j.getLocalId();
            if (lid != null && lid == next) {
                next++;
            } else if (lid == null || lid > next) {
                return next;
            }
        }
        return next;
    }

    /** 查询计划列表（按创建时间倒序）。createdBy 非空时仅返回 created_by = createdBy 的记录，实现按用户隔离 */
    public List<ScheduleJob> listByCreatedBy(Long createdBy) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScheduleJob> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScheduleJob>()
                        .orderByDesc(ScheduleJob::getCreateTime);
        if (createdBy != null) {
            wrapper.eq(ScheduleJob::getCreatedBy, createdBy);
        }
        return scheduleJobMapper.selectList(wrapper);
    }
}
