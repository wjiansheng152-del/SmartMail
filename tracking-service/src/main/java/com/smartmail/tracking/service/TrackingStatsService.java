package com.smartmail.tracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.smartmail.tracking.mapper.TrackingEventMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrackingStatsService {

    private final TrackingEventMapper trackingEventMapper;

    public Map<String, Object> getCampaignStats(Long campaignId) {
        int openCount = trackingEventMapper.countByCampaignAndType(campaignId, "open");
        int clickCount = trackingEventMapper.countByCampaignAndType(campaignId, "click");
        return Map.of(
                "campaignId", campaignId,
                "openCount", openCount,
                "clickCount", clickCount
        );
    }
}
