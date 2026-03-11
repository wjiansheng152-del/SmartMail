package com.smartmail.contact.web.dto;

import com.smartmail.contact.entity.ContactGroup;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分组 API 响应：对外 id 为租户内序号 local_id（连续、可复用）。
 */
@Data
public class ContactGroupResponse {

    private Integer id;
    private String name;
    private String ruleType;
    private String ruleExpr;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ContactGroupResponse from(ContactGroup g) {
        if (g == null) return null;
        ContactGroupResponse r = new ContactGroupResponse();
        r.setId(g.getLocalId());
        r.setName(g.getName());
        r.setRuleType(g.getRuleType());
        r.setRuleExpr(g.getRuleExpr());
        r.setCreateTime(g.getCreateTime());
        r.setUpdateTime(g.getUpdateTime());
        return r;
    }
}
