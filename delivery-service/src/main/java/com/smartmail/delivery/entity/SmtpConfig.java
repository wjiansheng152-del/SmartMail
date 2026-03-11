package com.smartmail.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 SMTP 配置实体，对应表 smtp_config。
 * 按用户存储，发送时使用活动创建人的配置。
 */
@Data
@TableName("smtp_config")
public class SmtpConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID，对应 platform.sys_user.id */
    private Long userId;
    private String host;
    private Integer port;
    private String username;
    /** 加密后的密码，GET 接口不返回明文 */
    private String passwordEncrypted;
    private String fromEmail;
    private String fromName;
    /** 是否使用 SSL：0-否 1-是 */
    private Boolean useSsl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
