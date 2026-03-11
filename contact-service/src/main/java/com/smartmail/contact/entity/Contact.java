package com.smartmail.contact.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 客户（联系人）实体，对应表 contact。
 * <p>
 * 用于邮件营销的收件人，邮箱唯一；与分组通过 contact_group_member 多对多关联。
 * </p>
 */
@Data
@TableName("contact")
public class Contact {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户标识，与 TenantContext 一致，用于多租户数据隔离 */
    private String tenantId;
    /** 租户内序号，从 1 连续，删除后可复用，与 tenant_id 唯一 */
    private Integer localId;
    /** 邮箱，同一租户内唯一，必填 */
    private String email;
    /** 姓名 */
    private String name;
    /** 手机号 */
    private String mobile;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
