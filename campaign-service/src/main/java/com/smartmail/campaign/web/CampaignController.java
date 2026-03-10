package com.smartmail.campaign.web;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.campaign.entity.Campaign;
import com.smartmail.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动管理：创建活动、按 ID 查询、全量列表；活动状态默认 draft，可配合调度与投递使用。
 */
@RestController
@RequestMapping("/api/campaign/campaign")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    /** 创建活动，自动设置 createTime、updateTime，status 默认 draft */
    @PostMapping
    public Result<Campaign> create(@RequestBody Campaign campaign) {
        campaign.setCreateTime(LocalDateTime.now());
        campaign.setUpdateTime(LocalDateTime.now());
        if (campaign.getStatus() == null) {
            campaign.setStatus("draft");
        }
        campaignService.save(campaign);
        return Result.ok(campaign);
    }

    /** 按 ID 查询活动，不存在则 404 */
    @GetMapping("/{id}")
    public Result<Campaign> get(@PathVariable Long id) {
        Campaign c = campaignService.getById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return Result.ok(c);
    }

    /** 全量更新活动，不存在则 404 */
    @PutMapping("/{id}")
    public Result<Campaign> update(@PathVariable Long id, @RequestBody Campaign campaign) {
        Campaign existing = campaignService.getById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        campaign.setId(id);
        campaign.setCreateTime(existing.getCreateTime());
        campaign.setUpdateTime(LocalDateTime.now());
        campaignService.updateById(campaign);
        return Result.ok(campaignService.getById(id));
    }

    /** 查询当前租户下全部活动列表 */
    @GetMapping("/list")
    public Result<List<Campaign>> list() {
        return Result.ok(campaignService.list());
    }
}
