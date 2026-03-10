package com.smartmail.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单条投递任务实体，对应表 delivery_task。
 * 每封待发邮件一条记录，用于状态回写与追踪关联（deliveryId）。
 */
@Data
@TableName("delivery_task")
public class DeliveryTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    /** 所属批次 ID，用于回写 success_count/fail_count */
    private Long batchId;
    private Long contactId;
    private String email;
    /** pending / sent / failed */
    private String status;
    private String channel;
    private String failReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
