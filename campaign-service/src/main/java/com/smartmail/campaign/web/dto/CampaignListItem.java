package com.smartmail.campaign.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动列表项：在 Campaign 基础上增加 sequence（当前用户下的展示序号，从 1 开始），
 * 用于前端表格「序号」列，避免使用全局自增 id 导致不同用户看到不连续的编号。
 */
@Data
public class CampaignListItem {

    private Long id;
    private String name;
    private Long templateId;
    private Long groupId;
    private Long createdBy;
    private String status;
    private String abConfig;
    private LocalDateTime scheduledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 当前用户列表中的展示序号，从 1 开始 */
    private int sequence;
}
