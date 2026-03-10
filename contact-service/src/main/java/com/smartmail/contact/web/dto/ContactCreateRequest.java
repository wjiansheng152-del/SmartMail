package com.smartmail.contact.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactCreateRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email
    private String email;
    private String name;
    private String mobile;
}
