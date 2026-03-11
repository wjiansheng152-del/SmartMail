package com.smartmail.campaign.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("campaign")
public class Campaign {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long templateId;
    private Long groupId;
    /** 创建人用户 ID（平台 sys_user.id），发送时使用该用户的 SMTP 配置 */
    private Long createdBy;
    private String status;
    private String abConfig;
    private LocalDateTime scheduledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
