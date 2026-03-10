package com.smartmail.contact.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("unsubscribe_list")
public class Unsubscribe {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String reason;
    private LocalDateTime createTime;
}
