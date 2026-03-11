package com.smartmail.scheduler.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动触发消息体：调度到点后发送到队列，由 delivery 消费并生成具体发送任务。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignTriggerPayload {

    /** 活动 ID（campaign 的 local_id） */
    private Long campaignId;
    /** 活动创建人用户 ID，delivery 请求 campaign 时传 X-User-Id */
    private Long createdBy;
    /** 租户 ID，用于数据源与下游请求头 */
    private String tenantId;
    /** 计划 ID，便于更新 schedule_job 状态 */
    private Long scheduleId;
}
