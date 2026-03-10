package com.smartmail.delivery.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动触发消息体（与 scheduler 发出格式一致）：campaignId、tenantId、scheduleId。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignTriggerPayload {

    private Long campaignId;
    private String tenantId;
    private Long scheduleId;
}
