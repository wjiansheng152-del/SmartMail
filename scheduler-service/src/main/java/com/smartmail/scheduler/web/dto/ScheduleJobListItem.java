package com.smartmail.scheduler.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送计划列表项：对外展示的计划 ID 为 local_id（按用户从 1 连续），非内部主键。
 */
@Data
public class ScheduleJobListItem {

    /** 计划 ID（local_id，按用户从 1 连续） */
    private Long id;
    private Long campaignId;
    private Long createdBy;
    private String cronExpr;
    private LocalDateTime runAt;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
