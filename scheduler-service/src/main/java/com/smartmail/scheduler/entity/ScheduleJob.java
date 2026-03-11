package com.smartmail.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时发送计划实体，对应表 schedule_job。
 * 支持一次性执行（runAt）或 Cron 表达式（cronExpr）。
 */
@Data
@TableName("schedule_job")
public class ScheduleJob {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 活动 ID（为 campaign 的 local_id，非内部主键） */
    private Long campaignId;
    /** 活动创建人用户 ID，投递时用于 X-User-Id 以便按 local_id 查询活动 */
    private Long createdBy;
    /** 创建人维度内序号，从 1 连续，删除后可复用；唯一约束 (created_by, local_id) */
    private Integer localId;
    /** Cron 表达式，与 runAt 二选一 */
    private String cronExpr;
    /** 单次执行时间，与 cronExpr 二选一 */
    private LocalDateTime runAt;
    /** 状态：pending-待执行 / running-执行中 / done-完成 / failed-失败 */
    private String status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
