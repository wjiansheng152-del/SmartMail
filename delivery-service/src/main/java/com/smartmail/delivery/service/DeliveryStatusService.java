package com.smartmail.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmail.delivery.entity.CampaignBatch;
import com.smartmail.delivery.mapper.CampaignBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按活动汇总投递状态：汇总该活动下所有批次的 total/success/fail。
 */
@Service
@RequiredArgsConstructor
public class DeliveryStatusService {

    private final CampaignBatchMapper campaignBatchMapper;

    /**
     * 按 campaignId 汇总各批次的 total_count、success_count、fail_count。
     */
    public Map<String, Object> getStatusByCampaignId(Long campaignId) {
        List<CampaignBatch> batches = campaignBatchMapper.selectList(
                new LambdaQueryWrapper<CampaignBatch>().eq(CampaignBatch::getCampaignId, campaignId)
        );
        int total = 0;
        int sent = 0;
        int failed = 0;
        for (CampaignBatch b : batches) {
            total += (b.getTotalCount() == null ? 0 : b.getTotalCount());
            sent += (b.getSuccessCount() == null ? 0 : b.getSuccessCount());
            failed += (b.getFailCount() == null ? 0 : b.getFailCount());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("campaignId", campaignId);
        map.put("total", total);
        map.put("sent", sent);
        map.put("failed", failed);
        return map;
    }
}
