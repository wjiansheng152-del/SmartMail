package com.smartmail.template.web.dto;

import com.smartmail.template.entity.EmailTemplate;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板 API 响应：对外 id 为租户内序号 local_id（连续、可复用）。
 */
@Data
public class TemplateResponse {

    private Integer id;
    private String name;
    private String subject;
    private String bodyHtml;
    private String variables;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static TemplateResponse from(EmailTemplate t) {
        if (t == null) return null;
        TemplateResponse r = new TemplateResponse();
        r.setId(t.getLocalId());
        r.setName(t.getName());
        r.setSubject(t.getSubject());
        r.setBodyHtml(t.getBodyHtml());
        r.setVariables(t.getVariables());
        r.setVersion(t.getVersion());
        r.setCreateTime(t.getCreateTime());
        r.setUpdateTime(t.getUpdateTime());
        return r;
    }
}
