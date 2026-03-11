package com.smartmail.contact.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户列表项：在 Contact 基础上增加 sequence（当前租户分页中的展示序号，从 1 开始），供前端表格「序号」列使用。
 */
@Data
public class ContactListItem {

    private Long id;
    private String tenantId;
    private String email;
    private String name;
    private String mobile;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 当前租户分页中的展示序号，从 1 开始（跨页连续） */
    private int sequence;
}
