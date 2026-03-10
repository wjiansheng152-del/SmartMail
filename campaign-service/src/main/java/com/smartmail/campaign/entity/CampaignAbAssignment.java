package com.smartmail.campaign.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("campaign_ab_assignment")
public class CampaignAbAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    private Long contactId;
    private String variant;
    private LocalDateTime createTime;
}
