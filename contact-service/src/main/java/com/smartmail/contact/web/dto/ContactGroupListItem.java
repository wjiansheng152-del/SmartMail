package com.smartmail.contact.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分组列表项：在 ContactGroup 基础上增加 sequence（当前租户下展示序号，从 1 开始），供前端表格「序号」列使用。
 */
@Data
public class ContactGroupListItem {

    private Long id;
    private String tenantId;
    private String name;
    private String ruleType;
    private String ruleExpr;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 当前租户列表中的展示序号，从 1 开始 */
    private int sequence;
}
