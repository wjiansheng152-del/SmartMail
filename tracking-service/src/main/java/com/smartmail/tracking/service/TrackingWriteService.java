package com.smartmail.tracking.service;

import com.smartmail.tracking.entity.TrackingEvent;
import com.smartmail.tracking.mapper.TrackingEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 写入打开/点击追踪事件到 tracking_event 表，供 stats 汇总。
 */
@Service
@RequiredArgsConstructor
public class TrackingWriteService {

    private final TrackingEventMapper trackingEventMapper;

    /** 记录一次打开事件 */
    public void recordOpen(Long campaignId, Long deliveryId) {
        TrackingEvent event = new TrackingEvent();
        event.setCampaignId(campaignId);
        event.setDeliveryId(deliveryId);
        event.setEventType("open");
        event.setLinkUrl(null);
        event.setCreateTime(LocalDateTime.now());
        trackingEventMapper.insert(event);
    }

    /** 记录一次点击事件 */
    public void recordClick(Long campaignId, Long deliveryId, String linkUrl) {
        TrackingEvent event = new TrackingEvent();
        event.setCampaignId(campaignId);
        event.setDeliveryId(deliveryId);
        event.setEventType("click");
        event.setLinkUrl(linkUrl != null && linkUrl.length() > 500 ? linkUrl.substring(0, 500) : linkUrl);
        event.setCreateTime(LocalDateTime.now());
        trackingEventMapper.insert(event);
    }
}
