package com.smartmail.delivery.web;

import com.smartmail.common.result.Result;
import com.smartmail.delivery.service.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 投递状态查询接口：按活动 ID 查询发送进度（总条数、已发送、失败数），数据来自 campaign_batch 汇总。
 */
@RestController
@RequestMapping("/api/delivery/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryStatusService deliveryStatusService;

    /** 查询指定活动的投递状态：campaignId、total、sent、failed */
    @GetMapping("/status/{campaignId}")
    public Result<Map<String, Object>> status(@PathVariable Long campaignId) {
        return Result.ok(deliveryStatusService.getStatusByCampaignId(campaignId));
    }
}
