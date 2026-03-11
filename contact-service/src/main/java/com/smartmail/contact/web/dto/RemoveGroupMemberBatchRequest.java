package com.smartmail.contact.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量从分组中移出客户的请求体，用于 DELETE /api/contact/group/{groupId}/member/batch。
 */
@Data
public class RemoveGroupMemberBatchRequest {

    /** 要移出分组的客户（联系人）id 列表，必填且不能为空 */
    @NotNull(message = "客户 id 列表不能为空")
    @NotEmpty(message = "客户 id 列表不能为空")
    private List<Long> contactIds;
}
