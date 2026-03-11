package com.smartmail.contact.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量将客户加入分组的请求体，用于 POST /api/contact/group/{groupId}/member/batch。
 */
@Data
public class AddGroupMemberBatchRequest {

    /** 要加入分组的客户（联系人）id 列表，必填且不能为空 */
    @NotNull(message = "客户 id 列表不能为空")
    @NotEmpty(message = "客户 id 列表不能为空")
    private List<Long> contactIds;
}
