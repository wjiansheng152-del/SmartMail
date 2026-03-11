package com.smartmail.template.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板列表项：在 EmailTemplate 基础上增加 sequence（当前租户下展示序号，从 1 开始），供前端表格「序号」列使用。
 */
@Data
public class TemplateListItem {

    private Long id;
    private String tenantId;
    private String name;
    private String subject;
    private String bodyHtml;
    private String variables;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 当前租户列表中的展示序号，从 1 开始 */
    private int sequence;
}
