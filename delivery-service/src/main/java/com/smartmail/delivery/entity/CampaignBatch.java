package com.smartmail.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送批次实体，对应表 campaign_batch。
 * 每次触发发送产生一个批次，汇总 total/success/fail 计数。
 */
@Data
@TableName("campaign_batch")
public class CampaignBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    private String batchNo;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
