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
    /** 邮箱，唯一，必填 */
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
