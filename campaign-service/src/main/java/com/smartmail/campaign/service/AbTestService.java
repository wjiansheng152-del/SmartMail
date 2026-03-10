package com.smartmail.campaign.service;

/**
 * A/B 测试：根据活动配置与联系人返回应使用的模板 ID（A 或 B 变体）。
 */
public interface AbTestService {

    /**
     * 获取该联系人在此活动中应使用的模板 ID。若活动无 A/B 配置则返回主模板 ID。
     *
     * @param campaignId 活动 ID
     * @param contactId  联系人 ID
     * @param mainTemplateId 主模板 ID（无 A/B 时直接返回）
     * @param abConfigJson A/B 配置 JSON，如 {"templateIdA":1,"templateIdB":2,"ratio":50}
     * @return 实际应使用的模板 ID
     */
    long getTemplateIdForContact(long campaignId, long contactId, long mainTemplateId, String abConfigJson);
}
