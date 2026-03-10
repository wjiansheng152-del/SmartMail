package com.smartmail.contact.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("contact_group")
public class ContactGroup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String ruleType;
    private String ruleExpr;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
