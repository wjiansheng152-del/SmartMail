package com.smartmail.contact.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新客户信息的请求体，用于 PUT /api/contact/contact/{id}。
 * <p>
 * email 必填且须符合邮箱格式；name、mobile 可选，可传 null 清空。
 * </p>
 */
@Data
public class ContactUpdateRequest {

    /** 邮箱，必填，须符合邮箱格式；若与其他客户重复则返回 409 */
    @NotBlank(message = "邮箱不能为空")
    @Email
    private String email;
    /** 姓名，可选 */
    private String name;
    /** 手机号，可选 */
    private String mobile;
}
