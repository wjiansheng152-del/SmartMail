package com.smartmail.template.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("email_template")
public class EmailTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String subject;
    private String bodyHtml;
    private String variables;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
