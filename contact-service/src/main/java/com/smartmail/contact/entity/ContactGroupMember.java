package com.smartmail.contact.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 分组-客户关联实体，对应表 contact_group_member。
 * <p>
 * 用于静态分组下将客户加入分组，多对多关联；表有唯一约束 uk_group_contact (group_id, contact_id)。
 * </p>
 */
@Data
@TableName("contact_group_member")
public class ContactGroupMember {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 分组 id */
    private Long groupId;
    /** 客户（联系人）id */
    private Long contactId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
