package com.smartmail.tracking.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tracking_event")
public class TrackingEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    private Long deliveryId;
    private String eventType;
    private String linkUrl;
    private LocalDateTime createTime;
}
