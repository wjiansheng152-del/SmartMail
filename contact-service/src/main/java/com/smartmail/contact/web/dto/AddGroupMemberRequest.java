package com.smartmail.contact.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 将客户加入分组的请求体，用于 POST /api/contact/group/{groupId}/member。
 */
@Data
public class AddGroupMemberRequest {

    /** 要加入分组的客户（联系人）id，必填 */
    @NotNull(message = "客户 id 不能为空")
    private Long contactId;
}
