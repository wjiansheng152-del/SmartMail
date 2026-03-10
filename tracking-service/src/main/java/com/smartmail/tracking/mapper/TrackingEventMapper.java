package com.smartmail.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.tracking.entity.TrackingEvent;
import org.apache.ibatis.annotations.Param;

public interface TrackingEventMapper extends BaseMapper<TrackingEvent> {

    int countByCampaignAndType(@Param("campaignId") Long campaignId, @Param("eventType") String eventType);
}
