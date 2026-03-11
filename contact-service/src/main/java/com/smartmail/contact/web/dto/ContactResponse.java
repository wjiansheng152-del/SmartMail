package com.smartmail.contact.web.dto;

import com.smartmail.contact.entity.Contact;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户 API 响应：对外 id 为租户内序号 local_id（连续、可复用），与创建时间相关。
 */
@Data
public class ContactResponse {

    /** 租户内序号，即 local_id，从 1 连续，删除后可被新记录复用 */
    private Integer id;
    private String email;
    private String name;
    private String mobile;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ContactResponse from(Contact c) {
        if (c == null) return null;
        ContactResponse r = new ContactResponse();
        r.setId(c.getLocalId());
        r.setEmail(c.getEmail());
        r.setName(c.getName());
        r.setMobile(c.getMobile());
        r.setCreateTime(c.getCreateTime());
        r.setUpdateTime(c.getUpdateTime());
        return r;
    }
}
