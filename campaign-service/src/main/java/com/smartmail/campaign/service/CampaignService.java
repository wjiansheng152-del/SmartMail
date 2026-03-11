package com.smartmail.campaign.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.campaign.entity.Campaign;

import java.util.List;

public interface CampaignService extends IService<Campaign> {

    /**
     * 删除营销活动：先删除同库 campaign_ab_assignment 关联，再删除 campaign 主记录；不存在也视为成功（幂等）。
     *
     * @param id 活动主键
     */
    void removeCampaign(Long id);

    /**
     * 查询活动列表：若传入 createdBy 则仅返回该用户创建的活动，否则返回当前租户下全部（兼容未传 X-User-Id 的调用）。
     *
     * @param createdBy 创建人用户 ID，可为 null
     * @return 活动列表
     */
    List<Campaign> listByCreatedBy(Long createdBy);

    /**
     * 为指定创建人分配下一个可复用的 local_id（最小未用正整数）。
     *
     * @param createdBy 创建人用户 ID，不可为 null
     * @return 下一个可用的 local_id
     */
    int nextLocalIdForCreatedBy(Long createdBy);

    /**
     * 按 local_id + 创建人查询活动。
     *
     * @param localId   对外序号（local_id）
     * @param createdBy 创建人用户 ID
     * @return 活动实体，不存在则 null
     */
    Campaign getByLocalIdAndCreatedBy(Integer localId, Long createdBy);

    /**
     * 按 local_id + 创建人删除活动（先删 A/B 分配再删主表）；不存在也视为成功（幂等）。
     *
     * @param localId   对外序号（local_id）
     * @param createdBy 创建人用户 ID
     */
    void removeByLocalIdAndCreatedBy(Integer localId, Long createdBy);
}
