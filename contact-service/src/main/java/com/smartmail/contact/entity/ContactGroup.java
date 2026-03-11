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
    /** 租户标识，用于多租户数据隔离 */
    private String tenantId;
    /** 租户内序号，从 1 连续，删除后可复用 */
    private Integer localId;
    private String name;
    private String ruleType;
    private String ruleExpr;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
